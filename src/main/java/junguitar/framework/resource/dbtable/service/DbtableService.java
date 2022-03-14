package junguitar.framework.resource.dbtable.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import junguitar.framework.resource.dbtable.dto.Column;
import junguitar.framework.resource.dbtable.dto.Table;
import junguitar.framework.resource.dbtable.service.table.TableService;
import junguitar.framework.resource.dbtable.util.DbtableUtils;
import junguitar.framework.resource.dbtable.util.DbtableUtils.RowData;
import junguitar.framework.resource.dbtable.util.DbtableUtils.SheetData;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DbtableService {
	@Autowired
	private TableService tableService;

	public String infoDiff(String schemaName, String externalSchemaName) {
		Collection<Table> content = DbtableUtils.getExternalCollection(externalSchemaName).getContent();

		Map<String, Table> schema = tableService.getMap(schemaName);
		Map<String, Table> eschema = new LinkedHashMap<>();
		Map<String, Column> cols = new LinkedHashMap<>();
		Map<String, Column> ecols = new LinkedHashMap<>();
		Map<String, List<String>> diffs = new LinkedHashMap<>();
		for (Table etable : content) {
			String tableName = etable.getName();
			if (!schema.containsKey(tableName)) {
				eschema.put(tableName, etable);
				continue;
			}

			// table diff
			Table table = schema.remove(tableName);
			putDiff(diffs, tableName, "attribute", "type", schemaName, table.getType(), externalSchemaName,
					etable.getType());
			putDiff(diffs, tableName, "attribute", "comment", schemaName, table.getComment(), externalSchemaName,
					etable.getComment());

			table.getColumns().forEach(col -> cols.put(col.getTableName() + "." + col.getName(), col));
			for (Column ecol : etable.getColumns()) {
				String key = ecol.getTableName() + "." + ecol.getName();
				if (!cols.containsKey(key)) {
					ecols.put(key, ecol);
					continue;
				}

				// column diff
				Column col = cols.remove(key);
				putDiff(diffs, tableName, col.getName(), "primaryKey", schemaName, col.isPrimaryKey(),
						externalSchemaName, ecol.isPrimaryKey());
				putDiff(diffs, tableName, col.getName(), "dataType", schemaName, col.getDataType(), externalSchemaName,
						ecol.getDataType());
				putDiff(diffs, tableName, col.getName(), "length", schemaName, col.getLength(), externalSchemaName,
						ecol.getLength());
				putDiff(diffs, tableName, col.getName(), "scale", schemaName, col.getScale(), externalSchemaName,
						ecol.getScale());
				putDiff(diffs, tableName, col.getName(), "ref", schemaName, col.isRef(), externalSchemaName,
						ecol.isRef());
				putDiff(diffs, tableName, col.getName(), "refTableName", schemaName, col.getRefTableName(),
						externalSchemaName, ecol.getRefTableName());
				putDiff(diffs, tableName, col.getName(), "refColumnName", schemaName, col.getRefColumnName(),
						externalSchemaName, ecol.getRefColumnName());
				putDiff(diffs, tableName, col.getName(), "comment", schemaName, col.getComment(), externalSchemaName,
						ecol.getComment());

			}
		}

		StringBuilder buf = new StringBuilder();

		if (!eschema.isEmpty()) {
			buf.append("\r\n").append(schemaName).append(" no such tables: ");
			eschema.keySet().forEach(name -> buf.append("\r\n\t").append(name));
		}
		if (!ecols.isEmpty()) {
			buf.append("\r\n").append(schemaName).append(" no such columns: ");
			ecols.keySet().forEach(name -> buf.append("\r\n\t").append(name));
		}
		if (!schema.isEmpty()) {
			buf.append("\r\n").append(externalSchemaName).append(" no such tables: ");
			schema.keySet().forEach(name -> buf.append("\r\n\t").append(name));
		}
		if (!cols.isEmpty()) {
			buf.append("\r\n").append(externalSchemaName).append(" no such columns: ");
			cols.keySet().forEach(name -> buf.append("\r\n\t").append(name));
		}

		if (!diffs.isEmpty()) {
			buf.append("\r\ndifferences: ");
			diffs.forEach((tableName, list) -> {
				buf.append("\r\n\t").append(tableName);
				list.forEach(message -> buf.append(message));
			});
		}

		String str = buf.toString();
		log.info(str);

		return str;
	}

	private static void putDiff(Map<String, List<String>> diff, String tableName, String type, String key,
			String schema1, Object value1, String schema2, Object value2) {
		if (value1 == null || value2 == null) {
			if (value1 == null && value2 == null) {
				return;
			}
			addDiff(diff, tableName, type + "." + key + ": " + schema1 + "=" + value1 + ", " + schema2 + "=" + value2);
		}

		if (value1.equals(value2)) {
			return;
		}
		addDiff(diff, tableName, "\r\n\t\t" + type + "." + key + ":\r\n\t\t\t" + schema1 + ": " + value1 + "\r\n\t\t\t"
				+ schema2 + ": " + value2);
	}

	private static void addDiff(Map<String, List<String>> diff, String tableName, String message) {
		List<String> list;
		if (diff.containsKey(tableName)) {
			list = diff.get(tableName);
		} else {
			list = new ArrayList<>();
			diff.put(tableName, list);
		}
		list.add(message);
	}

	/**
	 * log info about dbtables' metadata by schemaName of the tables.<br>
	 * If sheetPath and sheetName of spread sheet, which is already written, is input, It will get and use base information from the sheet.<br>
	 * Also if the dictionarySheetName is input together, It will get and use base dictionary from the sheet.<br>
	 * 
	 * @param schemaName         Schema Name
	 * @param sheetPath          Sheet Path (File Path)
	 * @param sheetName          Sheet Name of Tables
	 * @param dictionaySheetName Sheet Name of Dictionary
	 * @return info str of dbtables
	 */
	public String info(String schemaName, String sheetPath, String sheetName, String dictionaySheetName) {
		Map<String, Table> tables = tableService.getMap(schemaName);

		Map<String, List<String>> seqs = new LinkedHashMap<>();

		if (!ObjectUtils.isEmpty(sheetPath)) {
			if (!ObjectUtils.isEmpty(sheetName)) {
				SheetData sheetData = DbtableUtils.getSheetData(sheetPath, sheetName);
				Map<String, Integer> fieldsIndex = sheetData.getFieldsIndex();

				for (RowData data : sheetData.getRows()) {
					String table = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Table");
					String column = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Column");
					String div = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Div");
					String relTable = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Rel. Table");
					String relColumn = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Rel. Column");
					String comment = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Comment");

					boolean tableFlag = "T".equals(div);
					boolean viewFlag = "V".equals(div);

					if (ObjectUtils.isEmpty(table) || (!tableFlag && !viewFlag && ObjectUtils.isEmpty(column))
							|| !tables.containsKey(table)) {
						continue;
					}

					if (tableFlag || viewFlag) {
						if (!seqs.containsKey(table)) {
							seqs.put(table, new ArrayList<>());
						}

						if (!ObjectUtils.isEmpty(comment)) {
							tables.get(table).setComment(comment);
						}
						continue;
					}

					if (!seqs.containsKey(table)) {
						seqs.put(table, new ArrayList<>());
					}
					seqs.get(table).add(column);

					for (Column col : tables.get(table).getColumns()) {
						if (column.equals(col.getName())) {
							if (ObjectUtils.isEmpty(col.getRefTableName()) && !ObjectUtils.isEmpty(relTable)) {
								col.setRefTableName(relTable);
							}
							if (ObjectUtils.isEmpty(col.getRefColumnName()) && !ObjectUtils.isEmpty(relColumn)) {
								col.setRefColumnName(relColumn);
							}
							if (!ObjectUtils.isEmpty(comment)) {
								col.setComment(comment);
							}
						}
					}
				}
			}

			if (!ObjectUtils.isEmpty(dictionaySheetName)) {
				SheetData sheetData = DbtableUtils.getSheetData(sheetPath, dictionaySheetName);
				Map<String, Integer> fieldsIndex = sheetData.getFieldsIndex();

				Map<String, String> comments = new HashMap<>();
				for (RowData data : sheetData.getRows()) {
					String column = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Column");
					String dataType = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Data Type");
					int length = new BigDecimal(DbtableUtils.getFieldValueStr(data, fieldsIndex, "Length")).intValue();
					int scale = new BigDecimal(DbtableUtils.getFieldValueStr(data, fieldsIndex, "Scale")).intValue();
					String comment = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Comment");

					if (ObjectUtils.isEmpty(column) || ObjectUtils.isEmpty(dataType) || ObjectUtils.isEmpty(scale)
							|| ObjectUtils.isEmpty(scale) || ObjectUtils.isEmpty(comment)) {
						continue;
					}

					String key = toKey(column, dataType, length, scale);
					comments.put(key, comment);
				}

				if (!comments.isEmpty()) {
					for (Table table : tables.values()) {
						for (Column col : table.getColumns()) {
							String key = toKey(col.getName(), col.getDataType(), col.getLength(), col.getScale());
							if (comments.containsKey(key)) {
								col.setComment(comments.get(key));
							}
						}
					}
				}
			}
		}

		StringBuilder buf = new StringBuilder();

		// Header
		DbtableUtils.appendRow(buf, "No.", "Table", "Column", "Div", "Data Type", "Length", "Scale", "Rel. Table",
				"Rel. Column", "Rows", "Added", "Comment");

		// Rows
		int[] i = { 0 };

		if (!seqs.isEmpty()) {
			for (String name : seqs.keySet()) {
				name = name.toLowerCase();
				if (!tables.containsKey(name)) {
					continue;
				}
				Table table = tables.remove(name);

				// Columns
				Map<String, Column> cols = table.getColumns().stream()
						.collect(Collectors.toMap(Column::getName, col -> col, (x, y) -> y, LinkedHashMap::new));
				List<Column> list = seqs.get(name).stream().filter(cname -> cols.containsKey(cname))
						.map(cname -> cols.remove(cname)).collect(Collectors.toList());
				cols.values().forEach(col -> col.setAdded(true));
				list.addAll(cols.values());
				table.setColumns(list);

				appendTable(buf, table, ++i[0]);
			}
		}

		tables.values().forEach(table -> {
			table.setAdded(true);
			table.getColumns().forEach(col -> col.setAdded(true));
			appendTable(buf, table, ++i[0]);
		});

		String str = buf.toString();
		log.info(str);

		return str;
	}

	private static void appendTable(StringBuilder buf, Table table, int index) {
		// Table
		DbtableUtils.appendRow(buf,
				// No
				index,
				// Table
				table.getName(), null,
				// Div
				("VIEW".equals(table.getType()) ? "V" : "T"), null, null, null, null, null,
				// Rows
				table.getRows(),
				// Added
				table.isAdded() ? "O" : null,
				// Comment
				table.getComment());

		int[] i = { 0 };
		// Columns
		table.getColumns().forEach(col -> DbtableUtils.appendRow(buf,
				// No
				("'" + index + "." + ++i[0]),
				// Table
				table.getName(),
				// Column
				col.getName(),
				// Div
				(col.isPrimaryKey() ? "PK" : (col.isRef() ? "R" : (col.getRefTableName() != null ? "RC" : null))),
				// Data Type
				col.getDataType(),
				// Length
				col.getLength(),
				// Scale
				col.getScale(),
				// Rel. Table
				col.getRefTableName(),
				// Rel. Column
				col.getRefColumnName(),
				// Rows
				table.getRows(),
				// Added
				col.isAdded() ? "O" : null,
				// Comment
				col.getComment()));
	}

	/**
	 * log info about dbtables' columns' dataType and ditionary by schemaName of the tables and columns.<br>
	 * If sheetPath and sheetName of spread sheet, which is already written, is input, It will get and use base information from the sheet.<br>
	 * Also if the tablesSheetName is input together, It will get and use base columns' comments(only if still empty at dictionary) from the sheet.<br>
	 * 
	 * @param schemaName      Schema Name
	 * @param sheetPath       Sheet Path (File Path)
	 * @param sheetName       Sheet Name of Dictionary
	 * @param tablesSheetName Sheet Name of Tables
	 * @return info str of dbtables' columns' dataType and dictionary
	 */
	public String infoColumnsDictionaries(String schemaName, String sheetPath, String sheetName,
			String tablesSheetName) {
		Map<String, List<Column>> cols = getColumnsDictionaries(schemaName);

		if (!ObjectUtils.isEmpty(sheetPath)) {
			if (!ObjectUtils.isEmpty(sheetName)) {
				SheetData sheetData = DbtableUtils.getSheetData(sheetPath, sheetName);
				Map<String, Integer> fieldsIndex = sheetData.getFieldsIndex();

				for (RowData data : sheetData.getRows()) {
					String column = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Column");
					String dataType = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Data Type");
					int length = new BigDecimal(DbtableUtils.getFieldValueStr(data, fieldsIndex, "Length")).intValue();
					int scale = new BigDecimal(DbtableUtils.getFieldValueStr(data, fieldsIndex, "Scale")).intValue();
					String comment = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Comment");

					if (ObjectUtils.isEmpty(column) || ObjectUtils.isEmpty(dataType) || ObjectUtils.isEmpty(scale)
							|| ObjectUtils.isEmpty(scale) || ObjectUtils.isEmpty(comment)) {
						continue;
					}

					String key = toKey(column, dataType, length, scale);
					if (!cols.containsKey(key)) {
						continue;
					}

					cols.get(key).get(0).setComment(comment);
				}
			}

			if (!ObjectUtils.isEmpty(tablesSheetName)) {
				SheetData sheetData = DbtableUtils.getSheetData(sheetPath, tablesSheetName);
				Map<String, Integer> fieldsIndex = sheetData.getFieldsIndex();

				for (RowData data : sheetData.getRows()) {
					String comment = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Comment");
					if (ObjectUtils.isEmpty(comment)) {
						continue;
					}

					String div = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Div");
					boolean tableFlag = "T".equals(div);
					boolean viewFlag = "V".equals(div);
					if (tableFlag || viewFlag) {
						continue;
					}

					String column = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Column");
					if (ObjectUtils.isEmpty(column)) {
						continue;
					}

					String dataType = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Data Type");
					int length = new BigDecimal(DbtableUtils.getFieldValueStr(data, fieldsIndex, "Length")).intValue();
					int scale = new BigDecimal(DbtableUtils.getFieldValueStr(data, fieldsIndex, "Scale")).intValue();

					String key = toKey(column, dataType, length, scale);
					if (!cols.containsKey(key)) {
						continue;
					}

					Column col = cols.get(key).get(0);
					if (!ObjectUtils.isEmpty(col.getComment())) {
						continue;
					}

					col.setComment(comment);
				}
			}
		}

		StringBuilder buf = new StringBuilder();

		// Header
		DbtableUtils.appendRow(buf, "No.", "Column", "Data Type", "Length", "Scale", "Comment", "Tables");

		// Rows
		int i = 0;
		for (String key : cols.keySet()) {
			List<Column> list = cols.get(key);
			if (list.isEmpty()) {
				continue;
			}

			Column col = list.get(0);
			DbtableUtils.appendRow(buf, ++i, col.getName(), col.getDataType(), col.getLength(), col.getScale(),
					col.getComment());
			int j = 0;
			for (Column item : list) {
				buf.append(j++ == 0 ? "\t" : ", ").append(StringUtils.capitalize(item.getTableName()));
			}
		}

		String str = buf.toString();
		log.info(str);

		return str;
	}

	private Map<String, List<Column>> getColumnsDictionaries(String schemaName) {
		Map<String, Table> tables = tableService.getMap(schemaName);

		Map<String, List<Column>> map = new TreeMap<>();

		for (Table table : tables.values()) {
			for (Column col : table.getColumns()) {
				String key = toKey(col.getName(), col.getDataType(), col.getLength(), col.getScale());
				List<Column> list;
				if (map.containsKey(key)) {
					list = map.get(key);
				} else {
					list = new ArrayList<>();
					map.put(key, list);
				}
				list.add(col);
			}
		}

		return map;
	}

	private static String toKey(Object... objs) {
		String value = StringUtils.arrayToCommaDelimitedString(objs);
		return value;
	}

//	private static String values(Object... values) {
//		StringBuilder buf = new StringBuilder();
//		for (Object value : values) {
//			if (value == null || value.toString().isBlank()) {
//				continue;
//			}
//			if (!buf.isEmpty()) {
//				buf.append(", ");
//			}
//			buf.append(value);
//		}
//		return buf.toString();
//	}

}
