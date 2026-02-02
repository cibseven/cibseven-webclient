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
package org.cibseven.webapp.auth.utils;

import java.util.Base64;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.JwtTokenSettings;
import org.cibseven.webapp.auth.User;
import org.cibseven.webapp.auth.providers.JwtUserProvider.TokenSettings;
import org.cibseven.webapp.config.EngineRestProperties;
import org.cibseven.webapp.config.EngineRestSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for handling engine-specific JWT token operations.
 * Provides methods for extracting engine information from requests and tokens,
 * and determining the appropriate TokenSettings based on engine configuration.
 */
public class EngineTokenUtils {

	/**
	 * Sets the engine from the X-Process-Engine header to the user object.
	 * This should be called in login methods to store the engine with the user.
	 * 
	 * @param user The user object (must be CIBUser or subclass)
	 * @param request The HTTP request containing the X-Process-Engine header
	 */
	public static void setEngineFromRequest(User user, HttpServletRequest request) {
		if (user instanceof CIBUser) {
			String engine = request.getHeader("X-Process-Engine");
			((CIBUser) user).setEngine(engine);
		}
	}
	
	/**
	 * Gets the appropriate TokenSettings for a given engine ID.
	 * If the engine has a custom jwtSecret configured, returns settings with that secret.
	 * Otherwise, returns the default settings.
	 * 
	 * @param engineId The engine ID in format "url|path|engineName" for additional engines
	 * @param engineRestProperties The engine REST properties configuration
	 * @param defaultSettings The default token settings to use if no custom settings are found
	 * @param validMinutes Token validity duration in minutes
	 * @param prolongMinutes Token prolongation duration in minutes
	 * @return TokenSettings with the appropriate JWT secret
	 */
	public static TokenSettings getSettingsForEngine(
			String engineId, 
			EngineRestProperties engineRestProperties,
			TokenSettings defaultSettings,
			long validMinutes,
			long prolongMinutes) {
		
		if (engineId == null || engineId.isEmpty() || !engineId.contains("|")) {
			// Default engine or legacy format - use default settings
			return defaultSettings;
		}
		
		// Parse engine ID format: "url|path|engineName"
		String[] parts = engineId.split("\\|", 3);
		if (parts.length != 3) {
			return defaultSettings;
		}
		
		String engineUrl = parts[0];
		String enginePath = parts[1];
		
		// Look for matching additional engine configuration
		if (engineRestProperties != null && engineRestProperties.getAdditionalEngineRest() != null) {
			for (EngineRestSource source : engineRestProperties.getAdditionalEngineRest()) {
				if (matchesEngineSource(source, engineUrl, enginePath)) {
					if (source.getJwtSecret() != null && !source.getJwtSecret().isEmpty()) {
						// Use engine-specific JWT secret
						return new JwtTokenSettings(source.getJwtSecret(), validMinutes, prolongMinutes);
					}
					break;
				}
			}
		}
		
		// No custom secret found, use default
		return defaultSettings;
	}
	
	/**
	 * Checks if an EngineRestSource matches the given URL and path.
	 * 
	 * @param source The EngineRestSource to check
	 * @param url The URL to match against
	 * @param path The path to match against
	 * @return true if the source matches the URL and path, false otherwise
	 */
	private static boolean matchesEngineSource(EngineRestSource source, String url, String path) {
		if (source.getUrl() == null) {
			return false;
		}
		
		// Normalize URLs for comparison (remove trailing slashes)
		String sourceUrl = source.getUrl().endsWith("/") 
			? source.getUrl().substring(0, source.getUrl().length() - 1) 
			: source.getUrl();
		String compareUrl = url.endsWith("/") 
			? url.substring(0, url.length() - 1) 
			: url;
		
		if (!sourceUrl.equals(compareUrl)) {
			return false;
		}
		
		// Normalize paths for comparison
		String sourcePath = source.getPath() != null && !source.getPath().isEmpty() 
			? source.getPath() 
			: "/engine-rest";
		if (!sourcePath.startsWith("/")) {
			sourcePath = "/" + sourcePath;
		}
		String comparePath = path.startsWith("/") ? path : "/" + path;
		
		return sourcePath.equals(comparePath);
	}
	
	/**
	 * Extracts the engine ID from a JWT token without signature verification.
	 * This allows us to determine which secret to use for full verification.
	 * 
	 * @param token the JWT token (with or without Bearer prefix)
	 * @return the engine ID from the token's user claim, or null if not found
	 */
	public static String extractEngineFromToken(String token) {
		try {
			// Strip Bearer prefix if present
			String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
			
			// Parse the token without signature verification to peek at the payload
			String[] parts = cleanToken.split("\\.");
			if (parts.length != 3) {
				return null;
			}
			
			// Decode the payload (middle part)
			String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
			ObjectMapper mapper = new ObjectMapper();
			var claims = mapper.readTree(payload);
			
			// Extract user JSON and parse engine from it
			var userNode = claims.get("user");
			if (userNode != null && userNode.isTextual()) {
				var userData = mapper.readTree(userNode.asText());
				var engineNode = userData.get("engine");
				if (engineNode != null && engineNode.isTextual()) {
					return engineNode.asText();
				}
			}
		} catch (Exception e) {
			// If we can't extract the engine, fall back to default settings
		}
		return null;
	}
	
	/**
	 * Gets the effective TokenSettings for parsing a token.
	 * Extracts the engine from the token and returns the appropriate settings.
	 * 
	 * @param token the JWT token
	 * @param engineRestProperties The engine REST properties configuration
	 * @param defaultSettings the default settings to use if no engine-specific settings are found
	 * @param validMinutes Token validity duration in minutes
	 * @param prolongMinutes Token prolongation duration in minutes
	 * @return the effective TokenSettings for this token
	 */
	public static TokenSettings getEffectiveSettingsForToken(
			String token, 
			EngineRestProperties engineRestProperties,
			TokenSettings defaultSettings,
			long validMinutes,
			long prolongMinutes) {
		
		String engineId = extractEngineFromToken(token);
		return (engineId != null) 
			? getSettingsForEngine(engineId, engineRestProperties, defaultSettings, validMinutes, prolongMinutes) 
			: defaultSettings;
	}
}
