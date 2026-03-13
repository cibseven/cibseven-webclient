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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import org.cibseven.webapp.rest.InfoService;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Autowired
	private InfoService infoService;

	@Value("${springdoc.cibseven-webclient.serverUrl:}")
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
				new Info().title(apiTitle).description(apiDescription).version(infoService.getImplementationVersion())
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
