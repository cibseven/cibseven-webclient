package org.cibseven.config;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.cibseven.rest.InfoService;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

	@Value("${springdoc.flowWebclient.serverUrl:}")
	String serverUrl;
	@Value("${api.common.title:}")
	String apiTitle;
	@Value("${api.common.description:}")
	String apiDescription;
	@Value("${api.common.termsOfService:}")
	String apiTermsOfService;
	@Value("${api.common.license:}")
	String apiLicense;
	@Value("${api.common.licenseUrl:}")
	String apiLicenseUrl;
	@Value("${api.common.contact.name:}")
	String apiContactName;
	@Value("${api.common.contact.url:}")
	String apiContactUrl;
	@Value("${api.common.contact.email:}")
	String apiContactEmail;

	@Bean
	OpenAPI getOpenApiDocumentation() {
		OpenAPI openAPI = new OpenAPI();

		if (serverUrl != null && !serverUrl.isEmpty()) {
			Server server = new Server();
			server.setUrl(serverUrl);
			openAPI.setServers(Arrays.asList(server));
		}

		return openAPI.info(
			new Info().title(apiTitle).description(apiDescription).version(new InfoService().getImplementationVersion())
			.contact(new Contact().name(apiContactName).url(apiContactUrl).email(apiContactEmail))
			.termsOfService(apiTermsOfService).license(new License().name(apiLicense).url(apiLicenseUrl)));
	}

    @Bean
    OpenApiCustomizer sortSchemasAlphabetically() {
		return openApi -> {
			Map<String, Schema> schemas = openApi.getComponents().getSchemas();
			openApi.getComponents().setSchemas(new TreeMap<>(schemas));
		};
	}
}