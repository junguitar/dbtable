package junguitar.framework.resource.dbtable.service.comment;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import junguitar.framework.resource.dbtable.util.DbtableUtils;
import junguitar.framework.resource.dbtable.util.DbtableUtils.RowData;
import junguitar.framework.resource.dbtable.util.DbtableUtils.SheetData;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CommentService {
	@Autowired
	private ApplicationContext beans;

	public String infoCommentListByExcel(String path, String sheetName) {
		SheetData sheetData = DbtableUtils.getSheetData(path, sheetName);
		Map<String, Integer> fieldsIndex = sheetData.getFieldsIndex();

		StringBuilder buf = new StringBuilder();
		DbtableUtils.appendRow(buf, "No.", "Table", "Column", "Character", "Comment");

		int i = 0;
		for (RowData data : sheetData.getRows()) {
			String table = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Table");
			String column = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Column");
			String div = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Character");
			String comment = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Comment");

			boolean tableFlag = "T".equals(div);
			boolean viewFlag = "V".equals(div);

			if (ObjectUtils.isEmpty(comment) || ObjectUtils.isEmpty(table)
					|| (!tableFlag && !viewFlag && ObjectUtils.isEmpty(column))) {
				continue;
			}

			DbtableUtils.appendRow(buf, ++i, table, column, div, comment);
		}

		String str = buf.toString();
		log.info(str);
		return str;
	}

	public void putCommentListByExcel(String path, String sheetName) {
		SheetData sheetData = DbtableUtils.getSheetData(path, sheetName);
		Map<String, Integer> fieldsIndex = sheetData.getFieldsIndex();

		for (RowData data : sheetData.getRows()) {
			String table = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Table");
			String column = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Column");
			String div = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Character");
			String dataType = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Data Type");
			String comment = DbtableUtils.getFieldValueStr(data, fieldsIndex, "Comment");

			boolean tableFlag = "T".equals(div);
			boolean viewFlag = "V".equals(div);

			if (ObjectUtils.isEmpty(comment) || ObjectUtils.isEmpty(table)
					|| (!tableFlag && !viewFlag && ObjectUtils.isEmpty(column))) {
				continue;
			}

			if (tableFlag || viewFlag) {
				beans.getBean(CommentService.class).updateTableComment(table, comment);
			} else {
				beans.getBean(CommentService.class).updateColumnComment(table, column, dataType, comment);
			}
		}
	}

	@Autowired
	private NamedParameterJdbcOperations npjo;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateTableComment(String table, String comment) {
		Map<String, Object> params = new HashMap<>();
		params.put("comment", comment);
		npjo.update("alter table " + table + " comment = :comment", params);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateColumnComment(String table, String column, String dataType, String comment) {
		Map<String, Object> params = new HashMap<>();
		params.put("comment", comment);
		npjo.update("alter table " + table + " change column " + column + " " + dataType + " comment = :comment",
				params);
	}
}
