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
 * Configuration properties for engine-specific REST endpoint mappings.
 * Allows configuring different URLs and paths for different engine instances.
 * 
 * The mappings configuration is OPTIONAL. If not configured, the application will use
 * the default engineRest.url and engineRest.path for all engines.
 * 
 * When mappings are configured, each mapping requires:
 * - mappingId (REQUIRED): The local engine name shown in the UI
 * - engineName (REQUIRED): The remote engine name on the target server
 * - url (REQUIRED): The base URL for the engine REST API
 * - path (REQUIRED): The REST API path
 * - displayName (optional): User-friendly name for display
 * - tooltip (optional): Additional information shown on hover
 * 
 * Example configuration in application.yaml:

 * cibseven:
 *   webclient:
 *     engineRest:
 *       url: http://localhost:8080
 *       path: /engine-rest
 *       mappings:
 *         - mappingId: dev-server
 *           engineName: default
 *           displayName: Dev Server
 *           tooltip: Dev production environment
 *           url: https://dev.cib.de/
 *           path: /engine-rest
 *         - mappingId: local-server
 *           engineName: default
 *           displayName: Local Server
 *           tooltip: Local production environment
 *           url: http://localhost:8080
 *           path: /engine-rest
 *
 * This allows multiple local engines to connect to remote engines with the same name.
 */
@Data
@Component
@ConfigurationProperties(prefix = "cibseven.webclient.engine-rest")
public class EngineRestMappingProperties {
	/**
	 * List of engine-specific REST endpoint mappings.
	 * Each mapping defines a unique engine name with its corresponding URL and path.
	 */
	private List<EngineRestMapping> mappings = new ArrayList<>();
}
