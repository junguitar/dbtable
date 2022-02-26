package junguitar.framework.resource.dbtable.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
@Profile("!prod")
public class SwaggerConfig {

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.OAS_30).select()
				.apis(RequestHandlerSelectors.basePackage("junguitar.framework.resource.dbtable"))
				.paths(PathSelectors.ant("/**")).build()
				.apiInfo(new ApiInfoBuilder().title("DB Table API Documentation")
						.description("DB Table Framework Provides API for Reflecting DB Schema.")
						.version("0.0.1-SNAPSHOT").build());
	}

}
