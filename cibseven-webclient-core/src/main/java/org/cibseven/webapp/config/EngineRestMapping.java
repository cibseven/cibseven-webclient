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

/**
 * Configuration class for mapping engine names to their specific REST endpoints.
 * Allows overriding the default engine REST URL and path for specific engines.
 */
@Data
public class EngineRestMapping {
	/**
	 * The name of the engine (e.g., "production", "development")
	 */
	private String engineName;
	
	/**
	 * The base URL for this engine's REST API (e.g., "http://localhost:8080")
	 */
	private String url;
	
	/**
	 * The REST API path for this engine (e.g., "/engine-rest")
	 */
	private String path;
}
