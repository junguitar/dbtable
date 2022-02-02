package junguitar.framework.dbtable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
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
	private DbtableService ds;

	@GetMapping("info")
	public String info(@RequestParam(required = true) String schemaName,
			@RequestParam(required = false) String sequence) {
		Map<String, Table> tables = ds.getTables(schemaName);

		StringBuilder buf = new StringBuilder();

		// Head
		append(buf, "No", "Table", "Column", "Character", "Data Type", "Length", "Scale", "Rel. Table", "Rel. Column",
				"Rows", "Comment");

		// Rows
		int[] i = { 0 };

		if (sequence != null && !sequence.isBlank()) {
			for (String name : StringUtils.tokenizeToStringArray(sequence, ",")) {
				name = name.toLowerCase();
				if (!tables.containsKey(name)) {
					continue;
				}
				Table table = tables.remove(name);
				appendTable(buf, table, ++i[0]);
			}
		}

		tables.values().forEach(table -> {
			appendTable(buf, table, ++i[0]);
		});

		String str = buf.toString();
		log.info(str);

		return str;
	}

	private static void appendTable(StringBuilder buf, Table table, int index) {
		// Table
		append(buf, index,
				// Table
				table.getName(), null,
				// Character
				("VIEW".equals(table.getType()) ? "V" : "T"), null, null, null, null, null,
				// Rows
				table.getRows(),
				// Comment
				table.getComment());

		int[] i = { 0 };
		// Columns
		table.getColumns().forEach(col -> append(buf,
				// No
				("'" + index + "." + ++i[0]),
				// Table
				table.getName(),
				// Column
				col.getName(),
				// Character
				(col.isPrimaryKey() ? "PK" : (col.getRefTableName() != null ? "R" : null)),
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
				// Comment
				col.getComment()));
	}

	@GetMapping("/columns/dictionaries/info")
	public String infoColumnsDictionaries(@RequestParam(required = true) String schemaName) {
		Map<String, List<Column>> map = getColumnsDictionaries(schemaName);

		StringBuilder buf = new StringBuilder();

		// Head
		append(buf, "No", "Column", "Data Type", "Length", "Scale", "Comment", "Tables");

		// Rows
		int i = 0;
		for (String key : map.keySet()) {
			List<Column> list = map.get(key);
			if (list.isEmpty()) {
				continue;
			}

			Column col = list.get(0);
			append(buf, ++i, col.getName(), col.getDataType(), col.getLength(), col.getScale(), col.getComment());
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
		Map<String, Table> tables = ds.getTables(schemaName);

		Map<String, List<Column>> map = new TreeMap<>();

		for (Table table : tables.values()) {
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

	private static void append(StringBuilder buf, Object... values) {
		int i = 0;
		for (Object value : values) {
			buf.append(i++ == 0 ? "\r\n" : "\t");
			if (value != null) {
				buf.append(value);
			}
		}
	}
}
