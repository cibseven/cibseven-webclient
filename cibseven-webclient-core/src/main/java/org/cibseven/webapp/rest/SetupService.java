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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.UserGroup;
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

	private static final String ADMIN_GROUP_ID = "camunda-admin";
	private static final String ADMIN_GROUP_NAME = "camunda BPM Administrators";
	private static final String ADMIN_GROUP_TYPE = "SYSTEM";
	
	// Authorization type: 1 = GRANT
	private static final int AUTH_TYPE_GRANT = 1;
	// All permissions
	private static final String[] ALL_PERMISSIONS = new String[] { "ALL" };

	@Autowired BpmProvider bpmProvider;
	
	SevenProvider sevenProvider;
	
	@Override
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else
			throw new SystemException("SetupService expects a SevenProvider");
	}

	/**
	 * Check if initial setup is required (no users exist in the system).
	 * This endpoint does NOT require authentication.
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
		return getAdminGroupMemberCount(engine) == 0;
	}

	/**
	 * Create the initial admin user. This endpoint only works when no users exist.
	 * This endpoint does NOT require authentication.
	 * 
	 * @param newUser The user to create with profile and credentials
	 * @param engine The process engine to use (from X-Process-Engine header)
	 * @return Success response or error if users already exist
	 */
	@Operation(
		summary = "Create initial admin user",
		description = "Creates the first admin user. Only works when no users exist in the system.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "User created successfully"),
		@ApiResponse(responseCode = "403", description = "Setup not allowed - users already exist")
	})
	@PostMapping("/user")
	public ResponseEntity<Void> createInitialUser(
			@RequestBody NewUser newUser,
			@RequestHeader(value = "X-Process-Engine", required = false) String engine) {
		if (getAdminGroupMemberCount(engine) > 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		String userId = newUser.getProfile().getId();
		
		// 1. Create the admin user
		sevenProvider.createUser(newUser, null);
		
		// 2. Create the admin group if it doesn't exist
		if (!adminGroupExists()) {
			UserGroup adminGroup = new UserGroup(ADMIN_GROUP_ID, ADMIN_GROUP_NAME, ADMIN_GROUP_TYPE);
			sevenProvider.createGroup(adminGroup, null);
			
			// Create authorizations for admin group on all resource types (only if we created the group)
			createAdminAuthorizations();
		}
		
		// 3. Add user to admin group
		sevenProvider.addMemberToGroup(ADMIN_GROUP_ID, userId, null);
		
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}
	
	/**
	 * Checks if the admin group already exists.
	 */
	private boolean adminGroupExists() {
		try {
			Collection<UserGroup> groups = sevenProvider.findGroups(
				Optional.of(ADMIN_GROUP_ID),  // id
				Optional.empty(),              // name
				Optional.empty(),              // nameLike
				Optional.empty(),              // type
				Optional.empty(),              // member
				Optional.empty(),              // memberOfTenant
				Optional.empty(),              // sortBy
				Optional.empty(),              // sortOrder
				Optional.empty(),              // firstResult
				Optional.of("1"),              // maxResults
				null
			);
			return groups != null && !groups.isEmpty();
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Creates GRANT authorizations for the admin group on all resource types.
	 * Skips any authorizations that already exist to avoid duplicate key errors.
	 */
	private void createAdminAuthorizations() {
		// Fetch existing authorizations for the admin group
		Collection<Authorization> existingAuths = sevenProvider.findAuthorization(
			Optional.empty(),                      // id
			Optional.of(String.valueOf(AUTH_TYPE_GRANT)), // type: GRANT
			Optional.empty(),                      // userIdIn
			Optional.of(ADMIN_GROUP_ID),           // groupIdIn
			Optional.empty(),                      // resourceType
			Optional.empty(),                      // resourceId
			Optional.empty(),                      // sortBy
			Optional.empty(),                      // sortOrder
			Optional.empty(),                      // firstResult
			Optional.empty(),                      // maxResults
			null
		);
		
		// Build a set of existing resource types for quick lookup
		Set<Integer> existingResourceTypes = existingAuths.stream()
			.map(Authorization::getResourceType)
			.collect(Collectors.toSet());
		
		// Create only missing authorizations
		for (SevenResourceType resourceType : SevenResourceType.values()) {
			if (existingResourceTypes.contains(resourceType.getType())) {
				continue; // Skip if authorization already exists
			}
			
			Authorization auth = new Authorization(
				null,                           // id (generated by engine)
				AUTH_TYPE_GRANT,                // type: GRANT
				ALL_PERMISSIONS,                // permissions: ALL
				null,                           // userId (null, we use groupId)
				ADMIN_GROUP_ID,                 // groupId
				resourceType.getType(),         // resourceType
				"*"                             // resourceId: all resources
			);
			sevenProvider.createAuthorization(auth, null);
		}
	}
	
	/**
	 * Get the count of users that are members of the admin group.
	 * If no users are members of the admin group, setup is required.
	 */
	private long getAdminGroupMemberCount(String engine) {
		try {
			return sevenProvider.countUsers(
				Map.of("memberOfGroup", ADMIN_GROUP_ID),
				null
			);
		} catch (Exception e) {
			// If we can't query users, assume setup is needed
			return 0;
		}
	}
}
