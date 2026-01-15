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

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.rest.model.VariableInstance;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST service for managing variable instances.
 * 
 * Authorization Permissions Summary:
 * 
 * For Variable Instances (PROCESS_INSTANCE):
 * - READ: Required for retrieving variable instances.
 * - READ_INSTANCE: Required for accessing specific variable instance details.
 */
@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occurred"),
	@ApiResponse(responseCode = "401", description = "Unauthorized"),
	@ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
})
@RestController("WebclientVariableInstanceService") 
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/variable-instance")
public class VariableInstanceService extends BaseService implements InitializingBean {
	
	public void afterPropertiesSet() {
	}
	
	@Operation(
			summary = "Retrieves a variable instance by its ID.",
			description = "Retrieves a variable instance by its ID with optional value deserialization. " +
					"<strong>Return:</strong> Variable instance details")
	@ApiResponse(responseCode = "400", description = "There is at least one invalid parameter value")
	@ApiResponse(responseCode = "404", description = "Variable instance with the specified ID was not found")
	@GetMapping("/{id}")
	public VariableInstance getVariableInstance(
			@Parameter(description = "The ID of the variable instance to retrieve") 
			@PathVariable String id,
			@Parameter(description = "Whether to deserialize the variable value. If true, the value will be deserialized according to its type.")
			@RequestParam(required = false) Boolean deserializeValue,
			CIBUser user) {
		checkPermission(user, SevenResourceType.PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		boolean deserialize = (deserializeValue == null) || (deserializeValue != null && deserializeValue == true);
		return bpmProvider.getVariableInstance(id, deserialize, user);
	}
}
