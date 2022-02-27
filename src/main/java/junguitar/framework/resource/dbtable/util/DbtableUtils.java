package junguitar.framework.resource.dbtable.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import junguitar.framework.resource.dbtable.dto.Table;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DbtableUtils {
	private static DbtableUtils bean = null;

	public DbtableUtils() {
		bean = this;
	}

	@Value("${junguitar.tempDir}")
	private String tempDir;

	@Autowired
	@Qualifier(value = "externalSchemas")
	private Properties schemas;

	public static CollectionOut<Table> getExternalCollection(String externalSchemaName) {
		Assert.notEmpty(bean.schemas, "junguitar.external-schemas properties is required!!");
		Assert.notNull(externalSchemaName, "externalSchemaName is required!!");
		if (!bean.schemas.containsKey(externalSchemaName)) {
			throw new IllegalArgumentException(
					"junguitar.external-schemas." + externalSchemaName + " properties is required!!");
		}

		String url = bean.schemas.getProperty(externalSchemaName) + "/api/v1/framework/dbtables?schemaName="
				+ externalSchemaName;

		RestTemplate client = new RestTemplate();
		TableCollectionOut output = new TableCollectionOut();
		output = client.getForObject(url, TableCollectionOut.class);
		return output;
	}

	public static class TableCollectionOut extends CollectionOut<Table> {

	}

	public static void appendRow(StringBuilder buf, Object... values) {
		int i = 0;
		for (Object value : values) {
			buf.append(i++ == 0 ? "\r\n" : "\t");
			if (value != null) {
				buf.append(value);
			}
		}
	}

	public static SheetData getSheetData(String sheetPath, String sheetName) {
		Workbook workbook = getWorkbook(sheetPath);

		SheetData sheetData = new SheetData();
		try {
			Sheet sheet = workbook.getSheet(sheetName);

			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

			// Sheet Index
			int headerRowNum;
			int firstRowNum;
			int lastRowNum;
			{
				SheetIndex index = getSheetIndex(sheet, evaluator);
				if (index == null) {
					return sheetData;
				}
				headerRowNum = index.getHeaderRowNo();
				firstRowNum = index.getFirstRowNo();
				lastRowNum = index.getLastRowNo();
			}

			// Field Index
			Map<String, Integer> fieldsIndex;
			{
				Row header = sheet.getRow(headerRowNum);
				fieldsIndex = getFieldsIndex(header, evaluator);
				if (fieldsIndex.isEmpty() || !fieldsIndex.containsKey("Column")
						|| !fieldsIndex.containsKey("Comment")) {
					return sheetData;
				}
			}

			sheetData.setFieldsIndex(fieldsIndex);

			List<RowData> list = new ArrayList<>();

			for (int i = firstRowNum; i < lastRowNum + 1; i++) {
				final Row row = sheet.getRow(i);
				final RowData data = toData(row, evaluator, Arrays.asList());
				if (data == null) {
					continue;
				}

				list.add(data);
			}

			sheetData.setRows(list);
			return sheetData;
		} finally {
			closeQuietly(workbook);
			FileUtils.deleteQuietly(new File(toTempPath(sheetPath)));
		}
	}

	private static void closeQuietly(Closeable closeable) {
		try {
			closeable.close();
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		}
	}

	public static String getFieldValueStr(RowData data, Map<String, Integer> fieldsIndex, String name) {
		return getFieldValueStr(data, "value" + fieldsIndex.get(name));
	}

	private static SheetIndex getSheetIndex(Sheet sheet, FormulaEvaluator evaluator) {
		int headerRowNum = sheet.getFirstRowNum();
		int firstRowNum = headerRowNum + 2;
		int lastRowNum = sheet.getLastRowNum();
		if (lastRowNum < firstRowNum) {
			return null;
		}

		for (int i = headerRowNum; i < Math.min(10, lastRowNum + 1); i++) {
			final Row row = sheet.getRow(i);
			final RowData data = toData(row, evaluator, null);
			if (data == null) {
				continue;
			}

			boolean found = false;
			for (int j = 1; j < 5; j++) {
				String str = getFieldValueStr(data, "value" + j);
				if (str == null || str.isBlank() || !str.trim().equals("No.")) {
					continue;
				}
				found = true;
				headerRowNum = i;
				firstRowNum = headerRowNum + 1;
				if (lastRowNum < firstRowNum) {
					return new SheetIndex();
				}
				break;
			}

			if (found) {
				break;
			}
		}

		SheetIndex data = new SheetIndex();
		data.setHeaderRowNo(headerRowNum);
		data.setFirstRowNo(firstRowNum);
		data.setLastRowNo(lastRowNum);
		return data;
	}

	@Data
	private static class SheetIndex {
		private int headerRowNo;
		private int firstRowNo;
		private int lastRowNo;
	}

	private static Map<String, Integer> getFieldsIndex(Row header, FormulaEvaluator evaluator) {
		if (header == null) {
			return Collections.emptyMap();
		}

		Map<String, Integer> index = new LinkedHashMap<>();

		int firstCellNum = Math.max(0, header.getFirstCellNum());
		int lastCellNum = header.getLastCellNum() + 1;
		for (int i = firstCellNum; i < lastCellNum; i++) {
			Cell cell = header.getCell(i);
			Object value = getValue(cell, evaluator);
			if (value == null) {
				continue;
			}
			String colName = value.toString().trim();
			index.put(colName, (i + 1));
		}

		return index;
	}

	private static RowData toData(Row row, FormulaEvaluator evaluator, Collection<Integer> indexes) {
		if (row == null) {
			return null;
		}

		RowData data = new RowData();
		boolean found = false;
		int firstCellNum = Math.max(0, row.getFirstCellNum());
		int lastCellNum = row.getLastCellNum() + 1;
		if (indexes != null && !indexes.isEmpty()) {
			for (int index : indexes) {
				found = setFieldValue(data, row, index, evaluator) || found;
			}
		} else {
			for (int i = firstCellNum; i < lastCellNum; i++) {
				if (i == 100) {
					throw new RuntimeException("Column count is too big. It is restricted to 100");
				}

				int index = (i + 1);
				found = setFieldValue(data, row, index, evaluator) || found;
			}
		}

		return found ? data : null;
	}

	private static boolean setFieldValue(RowData data, Row row, int index, FormulaEvaluator evaluator) {
		if (index > 100) {
			throw new RuntimeException("Column count is too big. It is restricted to 100");
		}
		Cell cell = row.getCell(index - 1);
		Object value = getValue(cell, evaluator);
		if (value == null) {
			return false;
		}
		String str = value.toString();
		if (str.isBlank()) {
			return false;
		}

		setFieldValue(data, ("value" + index), str);
		return true;
	}

	private static String getFieldValueStr(Object data, String name) {
		Field field = getField(data.getClass(), name);
		Object value;
		try {
			value = field.get(data);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return (String) value;
	}

	private static void setFieldValue(Object data, String name, Object value) {
		Field field = getField(data.getClass(), name);
		try {
			field.set(data, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static Field getField(Class<?> clazz, String name) {
		Field field;
		try {
			field = clazz.getDeclaredField(name);
		} catch (NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
		return field;
	}

	private static Object getValue(Cell cell, FormulaEvaluator evaluator) {
		if (cell == null) {
			return null;
		}

		CellType cellType = cell.getCellType();

		if (evaluator != null && CellType.FORMULA.equals(cellType)) {
			cellType = evaluator.evaluateFormulaCell(cell);
		}

		Object value = null;
		if (CellType.BLANK.equals(cellType)) {
			return null;
		} else if (CellType.STRING.equals(cellType)) {
			value = cell.getStringCellValue();
		} else if (CellType.NUMERIC.equals(cellType)) {
			String str = cell.toString();
			int i1 = str.indexOf('-');
			int i2 = str.lastIndexOf('-');
			if (i1 != -1 && i1 != i2) {
				Date date = cell.getDateCellValue();
				return date;
			} else {
				value = cell.getNumericCellValue();
			}
		} else if (CellType.BOOLEAN.equals(cellType)) {
			value = cell.getBooleanCellValue();
		} else if (CellType.FORMULA.equals(cellType)) {
			value = cell.getCellFormula();
		} else if (CellType.ERROR.equals(cellType)) {
			value = cell.getErrorCellValue();
		} else {
			value = cell.getStringCellValue();
		}
		return value;
	}

	private static Workbook getWorkbook(String sheetPath) {
		File fileFrom = new File(sheetPath);
//		try {
//			fileFrom = ResourceUtils.getFile(sheetPath);
//		} catch (FileNotFoundException e) {
//			throw new RuntimeException("Cannot find the file: " + sheetPath);
//		}
		if (!fileFrom.exists()) {
			throw new RuntimeException("Cannot find the file: " + sheetPath);
		}

		File file = null;
		try {
			String path = toTempPath(sheetPath);
			file = new File(path);
			try {
				FileUtils.copyFile(fileFrom, file);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			Workbook workbook;
			try {
				workbook = WorkbookFactory.create(file);
			} catch (EncryptedDocumentException | IOException e) {
				throw new RuntimeException(e);
			}
			return workbook;

		} finally {
			FileUtils.deleteQuietly(file);
		}
	}

	private static String toTempPath(String path) {
		File file = new File(path);
		return bean.tempDir + "/" + file.getName();
	}

	@Data
	public static class SheetData {
		private Map<String, Integer> fieldsIndex = Collections.emptyMap();
		private List<RowData> rows = Collections.emptyList();
	}

	@Data
	public static class RowData {
		private String value1;
		private String value2;
		private String value3;
		private String value4;
		private String value5;
		private String value6;
		private String value7;
		private String value8;
		private String value9;
		private String value10;
		private String value11;
		private String value12;
		private String value13;
		private String value14;
		private String value15;
		private String value16;
		private String value17;
		private String value18;
		private String value19;
		private String value20;
		private String value21;
		private String value22;
		private String value23;
		private String value24;
		private String value25;
		private String value26;
		private String value27;
		private String value28;
		private String value29;
		private String value30;
		private String value31;
		private String value32;
		private String value33;
		private String value34;
		private String value35;
		private String value36;
		private String value37;
		private String value38;
		private String value39;
		private String value40;
		private String value41;
		private String value42;
		private String value43;
		private String value44;
		private String value45;
		private String value46;
		private String value47;
		private String value48;
		private String value49;
		private String value50;
		private String value51;
		private String value52;
		private String value53;
		private String value54;
		private String value55;
		private String value56;
		private String value57;
		private String value58;
		private String value59;
		private String value60;
		private String value61;
		private String value62;
		private String value63;
		private String value64;
		private String value65;
		private String value66;
		private String value67;
		private String value68;
		private String value69;
		private String value70;
		private String value71;
		private String value72;
		private String value73;
		private String value74;
		private String value75;
		private String value76;
		private String value77;
		private String value78;
		private String value79;
		private String value80;
		private String value81;
		private String value82;
		private String value83;
		private String value84;
		private String value85;
		private String value86;
		private String value87;
		private String value88;
		private String value89;
		private String value90;
		private String value91;
		private String value92;
		private String value93;
		private String value94;
		private String value95;
		private String value96;
		private String value97;
		private String value98;
		private String value99;
		private String value100;
	}

}
