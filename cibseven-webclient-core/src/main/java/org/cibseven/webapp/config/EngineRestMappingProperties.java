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
 * Example configuration in application.yaml:

 * cibseven:
 *   webclient:
 *     engineRest:
 *       url: http://localhost:8080
 *       path: /engine-rest
 *       mappings:
 *         - engineName: production
 *           url: http://prod-engine.example.com
 *           path: /engine-rest
 *         - engineName: development
 *           url: http://dev-engine.example.com
 *           path: /engine-rest
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
