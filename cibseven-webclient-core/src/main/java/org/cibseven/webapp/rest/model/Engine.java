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
package org.cibseven.webapp.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true) 
public class Engine {
	/**
	 * The actual engine name from the remote server (e.g., "default", "production")
	 */
	private String name;
	
	/**
	 * The base URL of the engine REST server this engine belongs to.
	 * Example: "http://localhost:8080", "https://dev.server.com"
	 */
	private String url;
	
	/**
	 * The REST API path for this engine.
	 * Example: "/engine-rest"
	 */
	private String path;
	
	/**
	 * Unique identifier for this engine, used for frontend storage (localStorage) and routing.
	 * Format: "{url}|{path}|{engineName}"
	 * Example: "http://localhost:8080|/engine-rest|default"
	 * This allows the frontend to uniquely identify engines even when multiple servers have the same engine name.
	 */
	private String id;
	
	/**
	 * Display name shown in the UI.
	 * Computed as: baseDisplayName + " (" + engineName + ")" if engine is not "default".
	 */
	private String displayName;
	
	/**
	 * Tooltip text shown on hover.
	 * Computed as: baseTooltip + " (" + engineName + ")" if engine is not "default".
	 */
	private String tooltip;

	public Engine(String name) {
		this.name = name;
	}

	public String json() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(this);
	}
}
