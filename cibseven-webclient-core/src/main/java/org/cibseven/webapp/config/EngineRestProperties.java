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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for engine REST endpoints.
 * Supports connecting to multiple engine REST servers.
 * 
 * Example configuration in application.yaml:
 *
 * cibseven:
 *   webclient:
 *     engineRest:
 *       url: http://localhost:8080
 *       path: /engine-rest
 *       displayName: Local Server
 *       tooltip: Local environment
 *       additionalEngineRest:
 *         - url: https://flow4cib.dev.cib.de/
 *           path: /engine-rest
 *           displayName: Dev Server
 *           tooltip: Development environment
 *         - url: https://flow4cib.qa.cib.de/
 *           path: /engine-rest
 *           displayName: QA Server
 *           tooltip: QA environment
 *
 * The application will fetch engines from the default engineRest URL and all
 * additionalEngineRest URLs, combining them into a single list.
 * 
 * For displayName and tooltip:
 * - If engine name is "default", uses displayName/tooltip as-is
 * - Otherwise, appends engine name in parentheses, e.g., "Local Server (production)"
 */
@Data
@Component
@ConfigurationProperties(prefix = "cibseven.webclient.engine-rest")
public class EngineRestProperties {
	/**
	 * The base URL for the default engine REST server (optional).
	 * Example: "http://localhost:8080", "https://dev.cib.de/"
	 * If not specified, falls back to @Value configuration.
	 */
	private String url;
	
	/**
	 * The REST API path for the default engine (optional).
	 * Example: "/engine-rest"
	 * If not specified, falls back to @Value configuration.
	 */
	private String path;
	
	/**
	 * Base display name for engines from the default REST endpoint (optional).
	 * The actual engine name will be appended in parentheses if not "default".
	 */
	private String displayName;
	
	/**
	 * Base tooltip text for engines from the default REST endpoint (optional).
	 * The actual engine name will be appended in parentheses if not "default".
	 */
	private String tooltip;
	
	/**
	 * List of additional engine REST sources to fetch engines from.
	 * Each source will be queried for available engines and added to the list.
	 */
	private List<EngineRestSource> additionalEngineRest = new ArrayList<>();
}
