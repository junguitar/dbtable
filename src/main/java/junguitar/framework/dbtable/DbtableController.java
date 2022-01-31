package junguitar.framework.dbtable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import junguitar.framework.dbtable.model.Table;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("api/framework/dbtables")
@Slf4j
public class DbtableController {
	@Autowired
	private DataSource ds;

	@Autowired
	private NamedParameterJdbcOperations npjo;

	@GetMapping("info")
	@Transactional
	public String infoDbtables(@RequestParam(required = true) String schemaName) {
		StringBuilder buf = new StringBuilder("tables:");

		Map<String, Object> params = new HashMap<>();
		params.put("schemaName", schemaName);
		Stream<Table> tableStream = npjo.queryForStream("SELECT * FROM information_schema.tables WHERE LOWER(table_schema) = LOWER(:schemaName) ORDER BY table_name", params,
				new RowMapper<Table>() {
					@Override
					public Table mapRow(ResultSet rs, int rowNum) throws SQLException {
						Table table = new Table();
						table.setTableName(rs.getString("table_name"));
						return table;
					}
				});

		tableStream.forEach((table) -> {
			buf.append("\r\n\t").append("table: ").append(table.getTableName());
		});

		String str = buf.toString();
		log.info(str);

		return str;
	}
}
