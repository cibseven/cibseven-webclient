/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.cibseven.webapp.config;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.cibseven.webapp.rest.InfoService;
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

	@Value("${cibseven.webclient.api.common.url}")
	String serverUrl;
	@Value("${cibseven.webclient.api.common.title:CIB seven webclient API}")
	String apiTitle;
	@Value("${cibseven.webclient.api.common.description:This API exposes the functionality of CIB seven webclient as a REST service under the Apache License 2.0.}")
	String apiDescription;
	@Value("${cibseven.webclient.api.common.termsOfService:https://www.apache.org/licenses/LICENSE-2.0}")
	String apiTermsOfService;
	@Value("${cibseven.webclient.api.common.license:Apache 2.0}")
	String apiLicense;
	@Value("${cibseven.webclient.api.common.licenseUrl:https://www.apache.org/licenses/LICENSE-2.0}")
	String apiLicenseUrl;
	@Value("${cibseven.webclient.api.common.contact.name:CIB seven}")
	String apiContactName;
	@Value("${cibseven.webclient.api.common.contact.url:https://cibseven.org}")
	String apiContactUrl;
	@Value("${cibseven.webclient.api.common.contact.email:info@cibseven.org}")
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