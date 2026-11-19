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

import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.rest.model.InfoVersion;
import org.springdoc.core.customizers.OpenApiCustomizer;
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

	private final SevenWebclientProperties properties;

	private final InfoVersion infoVersion;

	public OpenApiConfig(SevenWebclientProperties properties, InfoVersion infoVersion) {
		this.properties = properties;
		this.infoVersion = infoVersion;
	}
	
	@Bean
	OpenAPI getOpenApiDocumentation() {
		SevenWebclientProperties.Api.Common api = properties.getApi().getCommon();
		String serverUrl = api.getUrl();
		OpenAPI openAPI = new OpenAPI();

		if (serverUrl != null && !serverUrl.isEmpty()) {
			Server server = new Server();
			server.setUrl(serverUrl);
			openAPI.setServers(Arrays.asList(server));
		}

		return openAPI.info(
			new Info().title(api.getTitle()).description(api.getDescription()).version(infoVersion.getVersion())
			.contact(new Contact().name(api.getContact().getName()).url(api.getContact().getUrl()).email(api.getContact().getEmail()))
			.termsOfService(api.getTermsOfService()).license(new License().name(api.getLicense()).url(api.getLicenseUrl())));
	}

    @Bean
    OpenApiCustomizer sortSchemasAlphabetically() {
		return openApi -> {
			Map<String, Schema> schemas = openApi.getComponents().getSchemas();
			openApi.getComponents().setSchemas(new TreeMap<>(schemas));
		};
	}
}