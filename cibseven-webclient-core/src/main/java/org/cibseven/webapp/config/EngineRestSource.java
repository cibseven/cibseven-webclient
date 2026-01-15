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
 * Configuration for an engine REST endpoint source.
 * Defines where to fetch engine instances from.
 */
@Data
public class EngineRestSource {
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
	
	/**
	 * Base display name for engines from this REST endpoint (optional).
	 * The actual engine name will be appended in parentheses if not "default".
	 * Example: "Dev Server" -> displays as "Dev Server" for default engine, "Dev Server (production)" for production engine
	 */
	private String displayName;
	
	/**
	 * Base tooltip text for engines from this REST endpoint (optional).
	 * The actual engine name will be appended in parentheses if not "default".
	 */
	private String tooltip;
}
