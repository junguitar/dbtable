package junguitar.framework.resource.dbtable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import junguitar.framework.resource.dbtable.model.Column;
import junguitar.framework.resource.dbtable.model.Table;

@Service
@Transactional
public class DbtableService {
	@Autowired
	private NamedParameterJdbcOperations npjo;

	public Map<String, Table> getTables(String schemaName) {
		// Tables
		Map<String, Table> map = new LinkedHashMap<>();
//		SELECT * FROM information_schema.tables WHERE LOWER(table_schema) = LOWER(:schemaName) ORDER BY table_name
		Stream<Table> stream;
		{
			Map<String, Object> params = new HashMap<>();
			params.put("schemaName", schemaName);
			stream = npjo.queryForStream(
					"SELECT LOWER(table_name) name, table_type type, table_rows `rows`, table_comment comment FROM information_schema.tables WHERE LOWER(table_schema) = LOWER(:schemaName) ORDER BY table_name",
					params, new RowMapper<Table>() {
						@Override
						public Table mapRow(ResultSet rs, int rowNum) throws SQLException {
							Table table = new Table();
							table.setName(rs.getString("name"));
							table.setType(rs.getString("type"));
							table.setRows(rs.getLong("rows"));
							table.setComment(rs.getString("comment"));
							return table;
						}
					});
		}

		stream.forEach(table -> {
			map.put(table.getName(), table);

			// Columns
//			SELECT * FROM information_schema.columns WHERE LOWER(table_schema) = LOWER(:schemaName) AND LOWER(TABLE_NAME) = :tableName ORDER BY ordinal_position;
			Map<String, Column> cols = new LinkedHashMap<>();
			{
				Map<String, Object> params = new HashMap<>();
				params.put("schemaName", schemaName);
				params.put("tableName", table.getName());
				Stream<Column> cstream = npjo.queryForStream(
						"SELECT LOWER(column_name) column_name, LOWER(data_type) data_type, numeric_precision, datetime_precision, numeric_scale, column_key"
								+ " FROM information_schema.columns WHERE LOWER(table_schema) = LOWER(:schemaName) AND LOWER(TABLE_NAME) = :tableName ORDER BY ordinal_position",
						params, new RowMapper<Column>() {
							@Override
							public Column mapRow(ResultSet rs, int rowNum) throws SQLException {
								Column col = new Column();
								col.setTableName(table.getName());
								col.setName(rs.getString("column_name"));
								col.setDataType(rs.getString("data_type"));
								String key = rs.getString("column_key");

								col.setLength(
										Math.max(rs.getInt("numeric_precision"), rs.getInt("datetime_precision")));
								col.setScale(rs.getInt("numeric_scale"));

								if (key != null) {
									// PK
									if ("PRI".equals(key)) {
										col.setPrimaryKey(true);
									}
//									// Relation
//									else if ("MUL".equals(key)) {
//										col.setRefTableName("MUL");
//									}
								}

								return col;
							}
						});
				cstream.forEach(col -> {
					cols.put(col.getName(), col);
				});
			}

			// Relations
//			SELECT * FROM information_schema.key_column_usage WHERE LOWER(table_schema) = LOWER(:schemaName) AND LOWER(TABLE_NAME) = :tableName AND referenced_table_name is not null
			{
				Map<String, Object> params = new HashMap<>();
				params.put("schemaName", schemaName);
				params.put("tableName", table.getName());
				Stream<Column> cstream = npjo.queryForStream(
						"SELECT LOWER(column_name) column_name, LOWER(referenced_table_name) ref_table_name, LOWER(referenced_column_name) ref_column_name"
								+ " FROM information_schema.key_column_usage WHERE LOWER(table_schema) = LOWER(:schemaName)"
								+ " AND LOWER(TABLE_NAME) = :tableName AND referenced_table_name is not null",
						params, new RowMapper<Column>() {
							@Override
							public Column mapRow(ResultSet rs, int rowNum) throws SQLException {
								Column col = cols.get(rs.getString("column_name"));
								col.setRefTableName(rs.getString("ref_table_name"));
								col.setRefColumnName(rs.getString("ref_column_name"));
								return col;
							}
						});
				cstream.forEach(col -> {

				});
			}

			table.setColumns(new ArrayList<>(cols.values()));

		});

		return map;
	}
}