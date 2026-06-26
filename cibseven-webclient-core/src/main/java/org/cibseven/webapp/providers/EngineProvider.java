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
package org.cibseven.webapp.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.cibseven.webapp.config.EngineRestProperties;
import org.cibseven.webapp.config.EngineRestSource;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Engine;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.cibseven.webapp.rest.model.NewUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EngineProvider extends SevenProviderBase implements IEngineProvider {

	private static final String ENGINE_SUB_PATH = "/engine";

	@Getter @Setter
	private String effectiveDefaultEngineName = null;

	@Autowired(required = false)
	private EngineRestProperties engineRestProperties;

	private String getNamedEngineRestUrl(String engine) {
		String url = getEngineRestUrl();
		if (engine != null && !engine.isEmpty()) {
			// Parse engine ID format: "url|path|engineName"
			if (IEngineProvider.isExternalEngine(engine)) {
				String[] parts = engine.split("\\|", 3);
				if (parts.length == 3) {
					url = buildUrl(parts[0], parts[1]);
				} else {
					log.warn("Invalid engine ID format: {}, expected 'url|path|engineName'", engine);
				}
			}
			else if (!IEngineProvider.isNamedDefaultEngine(engine)) {
				// A specifically named engine (other than "default") lives at /engine/{name}.
				// The engine named "default" lives at the base /engine path.
				url += ENGINE_SUB_PATH + "/" + engine;
			}
		}
		return url;
	}

	@Override
	public Collection<Engine> getProcessEngineNames() {
		List<Engine> allEngines = new ArrayList<>();
		
		// Get default URL and path from properties, fallback to @Value fields from parent if not configured
		String defaultUrl = engineRestProperties != null && engineRestProperties.getUrl() != null 
			? engineRestProperties.getUrl() 
			: cibsevenUrl;
		String defaultPath = engineRestProperties != null && engineRestProperties.getPath() != null && !engineRestProperties.getPath().isEmpty()
			? engineRestProperties.getPath() 
			: engineRestPath;
		String defaultDisplayName = engineRestProperties != null ? engineRestProperties.getDisplayName() : null;
		String defaultTooltip = engineRestProperties != null ? engineRestProperties.getTooltip() : null;
		
		try {
			String fullUrl = buildUrl(defaultUrl, defaultPath) + ENGINE_SUB_PATH;
			Engine[] defaultEngines = ((ResponseEntity<Engine[]>) doGet(fullUrl, Engine[].class, null, false)).getBody();
			
			if (defaultEngines != null) {
				// Sort engines alphabetically by name within this source
				Arrays.sort(defaultEngines, (e1, e2) -> e1.getName().compareToIgnoreCase(e2.getName()));
				
				for (Engine engine : defaultEngines) {
					enrichEngine(engine, defaultUrl, defaultPath, defaultDisplayName, defaultTooltip, true);
					allEngines.add(engine);
				}
			}
		} catch (Exception e) {
			log.warn("Failed to fetch engines from default URL: {}", e.getMessage());
		}
		
		// Fetch engines from additional URLs
		if (engineRestProperties != null && engineRestProperties.getAdditionalEngineRest() != null) {
			for (EngineRestSource additional : engineRestProperties.getAdditionalEngineRest()) {
				try {
					String additionalPath = additional.getPath() != null && !additional.getPath().isEmpty() 
						? additional.getPath() 
						: "/engine-rest";
					String additionalUrl = buildUrl(additional.getUrl(), additionalPath) + ENGINE_SUB_PATH;
					Engine[] additionalEngines = ((ResponseEntity<Engine[]>) doGet(additionalUrl, Engine[].class, null, false)).getBody();
					
					if (additionalEngines != null) {
						// Sort engines alphabetically by name within this source
						Arrays.sort(additionalEngines, (e1, e2) -> e1.getName().compareToIgnoreCase(e2.getName()));
						
						for (Engine engine : additionalEngines) {
							enrichEngine(engine, additional.getUrl(), additionalPath, additional.getDisplayName(), additional.getTooltip(), false);
							allEngines.add(engine);
						}
					}
				} catch (Exception e) {
					log.warn("Failed to fetch engines from additional URL {}: {}", additional.getUrl(), e.getMessage());
				}
			}
		}
		
		return allEngines;
	}
	
	/**
	 * Enriches an engine with id, url, path, displayName, and tooltip.
	 * @param engine the engine to enrich
	 * @param sourceUrl the base URL of the engine REST server
	 * @param sourcePath the REST path of the engine server
	 * @param baseDisplayName the base display name from config
	 * @param baseTooltip the base tooltip from config
	 * @param isDefaultSource true if from default source, false if from additional source
	 */
	private void enrichEngine(Engine engine, String sourceUrl, String sourcePath, String baseDisplayName, String baseTooltip, boolean isDefaultSource) {
		// Store the URL and path so engine knows where it came from
		engine.setUrl(sourceUrl);
		engine.setPath(sourcePath);
		
		// Set ID format based on source:
		// - Default engine: simple format (just engine name) for backward compatibility and relative URLs in embedded forms
		// - Additional engine: full format (url|path|engineName) for unique identification across servers
		if (isDefaultSource) {
			engine.setId(engine.getName());
		} else {
			engine.setId(sourceUrl + "|" + sourcePath + "|" + engine.getName());
		}
		
		// Set displayName: append engine name in parentheses if not "default"
		if (baseDisplayName != null && !baseDisplayName.isEmpty()) {
			if (IEngineProvider.isNamedDefaultEngine(engine.getName())) {
				engine.setDisplayName(baseDisplayName);
			} else {
				engine.setDisplayName(baseDisplayName + " (" + engine.getName() + ")");
			}
		} else {
			engine.setDisplayName(engine.getName());
		}

		// Set tooltip: append engine name in parentheses if not "default"
		if (baseTooltip != null && !baseTooltip.isEmpty()) {
			if (IEngineProvider.isNamedDefaultEngine(engine.getName())) {
				engine.setTooltip(baseTooltip);
			} else {
				engine.setTooltip(baseTooltip + " (" + engine.getName() + ")");
			}
		}
	}

	@Override
	@Nullable
	public EngineConfiguration getEngineConfiguration(String engine) {
		String url = getNamedEngineRestUrl(engine) + "/configuration";
		try {
			return doGet(url, EngineConfiguration.class, null, false).getBody();
		} catch (SystemException e) {
			if (e.getCause() instanceof HttpClientErrorException.NotFound ||
				// for CIB seven before 2.2.0, with auth enabled, the endpoint returns 401 instead of 404 when not found
				e.getCause() instanceof HttpClientErrorException.Unauthorized) {
				log.warn("Engine configuration endpoint not found or secured at {}, falling back to legacy configuration", url);
				return null;
			}
			throw e;
		}
	}

	/**
	 * Builds a URL from base URL and path.
	 */
	private String buildUrl(String url, String path) {
		String baseUrl = url;
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		String restPath = path;
		if (!restPath.startsWith("/")) {
			restPath = "/" + restPath;
		}
		return baseUrl + restPath;
	}

	@Override
	public Boolean requiresSetup(String engine) {
		String url = getNamedEngineRestUrl(engine) + "/setup/status";
		return doGet(url, Boolean.class, null, false).getBody();
	}

	@Override
	public void createSetupUser(NewUser user, String engine) throws InvalidUserIdException {
		String url = getNamedEngineRestUrl(engine) + "/setup/user/create";
		try {
			//	A JSON object with the following properties:
			//	Name 	Type 	Description
			//	profile 	Array 	A JSON object containing variable key-value pairs. The object contains the following properties: id (String), firstName (String), lastName (String) and email (String).
			//	credentials 	Array 	A JSON object containing variable key-value pairs. The object contains the following property: password (String). 	 * 

			String body = "{\"profile\":"
					+ user.getProfile().json()
					+ ",\"credentials\":"
					+ user.getCredentials().json()
					+ "}";

			doPost(url, body , null, null);

		} catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in createUser(...):", se);
			throw se;
		}

	}
}
