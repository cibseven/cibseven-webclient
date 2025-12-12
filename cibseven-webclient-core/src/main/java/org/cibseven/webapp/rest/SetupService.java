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
package org.cibseven.webapp.rest;

import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.NewUser;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Setup service for initial system configuration.
 * These endpoints do NOT require authentication and are only accessible when no users exist in the system.
 */
@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occurred")
})
@RestController
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/setup")
public class SetupService extends BaseService implements InitializingBean {

	@Autowired BpmProvider bpmProvider;
  
  // Initial setup is only available for internal providers, not for external identity
	// providers like LDAP, ADFS, or SSO where users are managed externally.
	private static final String SEVEN_USER_PROVIDER = "org.cibseven.webapp.auth.SevenUserProvider";
	
	@Override
	public void afterPropertiesSet() {
	}

	/**
	 * Check if initial setup is required (no users exist in the system).
	 * This endpoint does NOT require authentication.
	 * 
	 * Setup is only required when using the internal SevenUserProvider.
	 * When external identity providers (LDAP, ADFS, etc.) are configured,
	 * setup is never required as users are managed externally.
	 * 
	 * @return true if setup is required, false otherwise
	 */
	@Operation(
		summary = "Check if initial setup is required",
		description = "Returns whether the system needs initial setup (no users exist)")
	@ApiResponse(responseCode = "200", description = "Setup status returned successfully")
	@GetMapping("/status")
	public boolean requiresSetup(
			@RequestHeader(value = "X-Process-Engine", required = false) String engine) {
    // Setup is only applicable when using internal user provider (SevenUserProvider)
		// For external identity providers (LDAP, ADFS, SSO), users are managed externally
		if (!SEVEN_USER_PROVIDER.equals(userProvider)) {
			return false;
		}
		return bpmProvider.requiresSetup(engine);
	}

	/**
	 * Create the initial admin user. This endpoint only works when no users exist
	 * and when using the internal SevenUserProvider.
	 * This endpoint does NOT require authentication.
	 * 
	 * The backend will handle group creation, authorization setup, and group membership.
	 * 
	 * @param newUser The user to create with profile and credentials
	 * @param engine The process engine to use (from X-Process-Engine header)
	 * @return Success response or error if users already exist or external provider is used
	 */
	@Operation(
		summary = "Create initial admin user",
		description = "Creates the first admin user. Only works when no users exist in the system. The backend handles group and authorization setup.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "User created successfully"),
		@ApiResponse(responseCode = "403", description = "Setup not allowed - users already exist or external identity provider is used")
	})
	@PostMapping("/user")
	public ResponseEntity<Void> createInitialUser(
			@RequestBody NewUser newUser,
			@RequestHeader(value = "X-Process-Engine", required = false) String engine) {
    // Setup is only applicable when using internal user provider (SevenUserProvider)
		if (!SEVEN_USER_PROVIDER.equals(userProvider)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		if (!bpmProvider.requiresSetup(engine)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		// Create the admin user - backend handles group and authorization setup
		bpmProvider.createSetupUser(newUser, engine);
		
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}
	
}
