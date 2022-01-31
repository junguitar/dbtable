package junguitar.framework.dbtable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import junguitar.framework.dbtable.model.Column;
import junguitar.framework.dbtable.model.Table;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("api/framework/dbtables")
@Slf4j
public class DbtableController {
	@Autowired
	private NamedParameterJdbcOperations npjo;

	@GetMapping("info")
	@Transactional
	public String info(@RequestParam(required = true) String schemaName) {
		List<Table> tables = getTables(schemaName);

		StringBuilder buf = new StringBuilder(
				"\r\nno\ttable\tcolumn\tdata type\tpk\tsize\tscale\trel table\trel column");

		int[] i = { 0 };
		int[] j = { 0 };

		tables.forEach(table -> {
			j[0] = 0;

			buf.append("\r\n").append(++i[0]).append("\t").append(table.getName());

			table.getColumns().forEach((col) -> {
				buf.append("\r\n").append(i[0]).append(".").append(++j[0]).append("\t\t").append(col.getName());
				buf.append("\t").append(col.getDataType());
				buf.append("\t").append(col.isPrimaryKey() ? "Y" : " ");
				buf.append("\t").append(col.getLength());
				buf.append("\t").append(col.getScale());
				if (col.getRefTableName() != null) {
					buf.append("\t").append(col.getRefTableName());
					if (col.getRefColumnName() != null) {
						buf.append("\t").append(col.getRefTableName());
					}
				}
			});

		});

		String str = buf.toString();
		log.info(str);

		return str;
	}

	@GetMapping("/columns/dictionaries/info")
	@Transactional
	public String infoColumnsDictionaries(@RequestParam(required = true) String schemaName) {
		Map<String, List<Column>> map = getColumnsDictionaries(schemaName);

		StringBuilder buf = new StringBuilder("\r\nno\tcolumn\tdata type\tsize\tscale\ttable");

		int i = 0;

		for (String key : map.keySet()) {
			List<Column> list = map.get(key);
			if (list.isEmpty()) {
				continue;
			}
			Column col = list.get(0);
			buf.append("\r\n").append(++i).append("\t").append(col.getName()).append("\t").append(col.getDataType())
					.append("\t").append(col.getLength()).append("\t").append(col.getScale());
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
		List<Table> tables = getTables(schemaName);

		Map<String, List<Column>> map = new TreeMap<>();

		for (Table table : tables) {
			for (Column col : table.getColumns()) {
				String key = col.getName() + "," + col.getDataType() + "," + col.getLength() + "," + col.getScale();
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

	private List<Table> getTables(String schemaName) {
		// Tables
		List<Table> list = new ArrayList<>();
//		SELECT * FROM information_schema.tables WHERE LOWER(table_schema) = LOWER(:schemaName) ORDER BY table_name
		Stream<Table> stream;
		{
			Map<String, Object> params = new HashMap<>();
			params.put("schemaName", schemaName);
			stream = npjo.queryForStream(
					"SELECT LOWER(table_name) as table_name FROM information_schema.tables WHERE LOWER(table_schema) = LOWER(:schemaName) ORDER BY table_name",
					params, new RowMapper<Table>() {
						@Override
						public Table mapRow(ResultSet rs, int rowNum) throws SQLException {
							Table table = new Table();
							table.setName(rs.getString("table_name"));
							return table;
						}
					});
		}

		stream.forEach(table -> {
			list.add(table);

			// Columns
//			SELECT * FROM information_schema.columns WHERE LOWER(table_schema) = LOWER(:schemaName) AND LOWER(TABLE_NAME) = :tableName ORDER BY ordinal_position;
			Map<String, Column> cols = new LinkedHashMap<>();
			{
				Map<String, Object> params = new HashMap<>();
				params.put("schemaName", schemaName);
				params.put("tableName", table.getName());
				Stream<Column> cstream = npjo.queryForStream(
						"SELECT LOWER(column_name) as column_name, LOWER(data_type) as data_type, numeric_precision, datetime_precision, numeric_scale, column_key"
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
						"SELECT LOWER(column_name) column_name, LOWER(referenced_table_name) as ref_table_name, LOWER(referenced_column_name) as ref_column_name"
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

		return list;
	}
}
