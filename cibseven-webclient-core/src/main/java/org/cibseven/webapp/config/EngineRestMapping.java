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
 * 
 * All fields marked as REQUIRED must be provided when defining a mapping.
 */
@Data
public class EngineRestMapping {
	/**
	 * The local engine name (REQUIRED).
	 * This is the name of the engine in your main/local environment that will be shown in the UI.
	 * It acts as the unique identifier for this mapping.
	 * Example: "dev-server", "local-server", "qa-environment"
	 */
	private String mappingId;
	
	/**
	 * The remote engine name (REQUIRED).
	 * This is the name of the engine on the remote server that this mapping points to.
	 * Example: "default", "production", "development"
	 */
	private String engineName;
	
	/**
	 * Custom display name for the engine (optional).
	 * Use this to provide a user-friendly name when multiple engines share the same engineName.
	 * If not provided, the mappingId will be used for display.
	 */
	private String displayName;
	
	/**
	 * Tooltip text for the engine (optional).
	 * Provides additional information about the engine when hovering over it in the UI.
	 */
	private String tooltip;
	
	/**
	 * The base URL for this engine's REST API (REQUIRED).
	 * Example: "http://localhost:8080", "https://dev.cib.de/"
	 */
	private String url;
	
	/**
	 * The REST API path for this engine (REQUIRED).
	 * Example: "/engine-rest"
	 */
	private String path;
}
