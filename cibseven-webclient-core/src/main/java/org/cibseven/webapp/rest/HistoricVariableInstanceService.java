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

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST service for managing historic variable instances.
 * 
 * Authorization Permissions Summary:
 * 
 * For Historic Variable Instances (PROCESS_INSTANCE):
 * - READ: Required for retrieving historic variable instances.
 * - READ_HISTORY: Required for accessing historic variable instance details.
 */
@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occurred"),
	@ApiResponse(responseCode = "401", description = "Unauthorized"),
	@ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
})
@RestController("WebclientHistoricVariableInstanceService") 
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/history/variable-instance")
public class HistoricVariableInstanceService extends BaseService implements InitializingBean {
	
	public void afterPropertiesSet() {
	}
	
	@Operation(
			summary = "Retrieves a historic variable instance by its ID.",
			description = "Retrieves a historic variable instance by its ID with optional value deserialization. " +
					"<strong>Return:</strong> Historic variable instance details")
	@ApiResponse(responseCode = "400", description = "There is at least one invalid parameter value")
	@ApiResponse(responseCode = "404", description = "Historic variable instance with the specified ID was not found")
	@GetMapping("/{id}")
	public VariableHistory getHistoricVariableInstance(
			@Parameter(description = "The ID of the historic variable instance to retrieve") 
			@PathVariable String id,
			@Parameter(description = "Whether to deserialize the variable value. If true, the value will be deserialized according to its type.")
			@RequestParam(required = false) Boolean deserializeValue,
			CIBUser user) {
		checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		boolean deserialize = (deserializeValue == null) || (deserializeValue != null && deserializeValue == true);
		return bpmProvider.getHistoricVariableInstance(id, deserialize, user);
	}

	@Operation(
			summary = "Query historic variable instances (engine-rest compatible path used by embedded forms)",
			description = "Filters are taken from the request body and forwarded to the engine's historic variable query, "
					+ "the way bpm-sdk sends them. <strong>Return:</strong> Collection of historic variable instances")
	@PostMapping("")
	public Collection<VariableHistory> queryHistoricVariableInstances(
			@Parameter(description = "Filters to apply to the historic variable query")
			@RequestBody(required = false) Map<String, Object> filters,
			@Parameter(description = "Index of the first result to return") @RequestParam Optional<Integer> firstResult,
			@Parameter(description = "Maximum number of results to return") @RequestParam Optional<Integer> maxResults,
			@Parameter(description = "Whether the values are deserialized; left to the engine when absent")
			@RequestParam(required = false) Boolean deserializeValues,
			CIBUser user) {
		checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.findHistoricVariableInstances(filters == null ? Map.of() : filters,
				firstResult, maxResults, deserializeValues, user);
	}

	@Operation(
			summary = "Count historic variable instances (engine-rest compatible path used by embedded forms)",
			description = "<strong>Return:</strong> JSON object with a single count property")
	@PostMapping("/count")
	public Map<String, Integer> queryHistoricVariableInstanceCount(
			@Parameter(description = "Filters to apply to the historic variable query")
			@RequestBody(required = false) Map<String, Object> filters,
			CIBUser user) {
		checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return Map.of("count", bpmProvider.findHistoricVariableInstancesCount(filters == null ? Map.of() : filters, user));
	}
}
