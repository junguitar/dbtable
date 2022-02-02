package junguitar.framework.resource.dbtable;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class DbtableConfig {

	@Autowired()
	private DataSource dataSource;

	@Bean
	public JdbcOperations jdbcOperations() {
		JdbcTemplate bean = new JdbcTemplate();
		bean.setDataSource(dataSource);
		bean.setFetchSize(100);
		bean.setMaxRows(10001);
		return bean;
	}

	@Bean
	public NamedParameterJdbcOperations namedParameterJdbcOperations() {
		NamedParameterJdbcTemplate bean = new NamedParameterJdbcTemplate(jdbcOperations());
		bean.setCacheLimit(200);
		return bean;
	}

}
