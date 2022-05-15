package junguitar.framework.resource.dbtable.service.table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import junguitar.framework.resource.dbtable.dto.Column;
import junguitar.framework.resource.dbtable.dto.Table;
import junguitar.framework.resource.dbtable.util.CollectionOut;
import junguitar.framework.resource.dbtable.util.DbtableUtils;
import junguitar.framework.resource.dbtable.util.DbtableUtils.TableCollectionOut;
import junguitar.framework.resource.dbtable.util.SchemaRef;

@Service
public class TableService {
	@Autowired
	private ApplicationContext beans;

	@Autowired
	private NamedParameterJdbcOperations npjo;

	@Transactional
	public CollectionOut<Table> getCollection(String schema) {
		SchemaRef ref = DbtableUtils.getSchemaRef(schema);
		if (ObjectUtils.isEmpty(ref.getLocation())) {
			return getLocalCollection(ref);
		} else {
			return getExternalCollection(ref);
		}
	}

	private CollectionOut<Table> getLocalCollection(SchemaRef ref) {
		CollectionOut<Table> output = new CollectionOut<>();
		Map<String, Table> map = getLocalMap(ref);
		if (map.isEmpty()) {
			return output;
		}
		output.setContent(Collections.unmodifiableCollection(map.values()));
		return output;
	}

	private CollectionOut<Table> getExternalCollection(SchemaRef ref) {
		String name = ref.getName();
		String url = ref.getLocation() + "/v1/framework/dbtables?schema=" + name;
		RestTemplate client = new RestTemplate();
		CollectionOut<Table> output = client.getForObject(url, TableCollectionOut.class);
		return output;
	}

	@Transactional
	public Map<String, Table> getMap(String schema) {
		SchemaRef ref = DbtableUtils.getSchemaRef(schema);
		if (ObjectUtils.isEmpty(ref.getLocation())) {
			return getLocalMap(ref);
		} else {
			return getExternalMap(ref);
		}
	}

	private Map<String, Table> getLocalMap(SchemaRef ref) {
		String name = ref.getName();

		// Tables
		Map<String, Table> map = new LinkedHashMap<>();
		// SELECT * FROM information_schema.tables WHERE LOWER(table_schema) =
		// LOWER(:schemaName) ORDER BY table_name
		Stream<Table> stream;
		{
			Map<String, Object> params = new HashMap<>();
			params.put("schemaName", name);
			stream = npjo.queryForStream("SELECT LOWER(tbl.table_name) name, tbl.table_type type, tbl.engine, "
					+ "ccs.character_set_name charset, tbl.table_collation collation, tbl.table_rows `rows`, tbl.table_comment comment "
					+ "FROM information_schema.tables tbl, information_schema.collation_character_set_applicability ccs "
					+ "WHERE tbl.table_collation = ccs.collation_name and LOWER(table_schema) = LOWER(:schemaName) ORDER BY table_name",
					params, new RowMapper<Table>() {
						@Override
						public Table mapRow(ResultSet rs, int rowNum) throws SQLException {
							Table table = new Table();
							table.setName(rs.getString("name"));
							table.setType(rs.getString("type"));
							table.setEngine(rs.getString("engine"));
							table.setCharset(rs.getString("charset"));
							table.setCollation(rs.getString("collation"));
							table.setRows(rs.getLong("rows"));
							table.setComment(rs.getString("comment"));
							return table;
						}
					});
		}

		stream.forEach(table -> {
			map.put(table.getName(), table);

			// Columns
			// SELECT * FROM information_schema.columns WHERE LOWER(table_schema) =
			// LOWER(:schemaName) AND LOWER(table_name) = :tableName ORDER BY
			// ordinal_position;
			Map<String, Column> cols = new LinkedHashMap<>();
			{
				Map<String, Object> params = new HashMap<>();
				params.put("schemaName", name);
				params.put("tableName", table.getName());
				Stream<Column> cstream = npjo.queryForStream(
						"SELECT LOWER(column_name) column_name, LOWER(data_type) data_type, character_maximum_length, numeric_precision, datetime_precision, numeric_scale, column_key"
								+ " FROM information_schema.columns WHERE LOWER(table_schema) = LOWER(:schemaName) AND LOWER(table_name) = :tableName ORDER BY ordinal_position",
						params, new RowMapper<Column>() {
							@Override
							public Column mapRow(ResultSet rs, int rowNum) throws SQLException {
								Column col = new Column();
								col.setTableName(table.getName());
								col.setName(rs.getString("column_name"));
								col.setDataType(rs.getString("data_type"));
								String key = rs.getString("column_key");
								Long length = Math.max(rs.getLong("character_maximum_length"),
										rs.getLong("numeric_precision"));
								if (length <= 0) {
									length = Math.max(rs.getLong("numeric_precision"),
											rs.getLong("datetime_precision"));
								}
								if (length < 10000) {
									col.setLength(length.intValue());
								}
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
			// SELECT * FROM information_schema.key_column_usage WHERE LOWER(table_schema) =
			// LOWER(:schemaName) AND LOWER(table_name) = :tableName AND
			// referenced_table_name is not null
			{
				Map<String, Object> params = new HashMap<>();
				params.put("schemaName", name);
				params.put("tableName", table.getName());
				Stream<Column> cstream = npjo.queryForStream(
						"SELECT LOWER(column_name) column_name, LOWER(referenced_table_name) ref_table_name, LOWER(referenced_column_name) ref_column_name"
								+ " FROM information_schema.key_column_usage WHERE LOWER(table_schema) = LOWER(:schemaName)"
								+ " AND LOWER(table_name) = :tableName AND referenced_table_name is not null",
						params, new RowMapper<Column>() {
							@Override
							public Column mapRow(ResultSet rs, int rowNum) throws SQLException {
								Column col = cols.get(rs.getString("column_name"));
								col.setRefTableName(rs.getString("ref_table_name"));
								col.setRefColumnName(rs.getString("ref_column_name"));
								col.setRef(col.getRefTableName() != null);
								return col;
							}
						});

				// Warning!!
				// Never delete this empty block.
				// If remove this block, previous logic won't be run.
				cstream.forEach(col -> {

				});
			}

			table.setColumns(new ArrayList<>(cols.values()));

		});

		return map;
	}

	private Map<String, Table> getExternalMap(SchemaRef ref) {
		Map<String, Table> map = new LinkedHashMap<>();
		getExternalCollection(ref).getContent().forEach(table -> map.put(table.getName(), table));
		return map;
	}

}
