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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.rest.model.HistoricDecisionInstance;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.Decision;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/*
 * Interface: BpmProvider
 *
 * Authorization Permissions Summary:
 * 
 * For Decision Definitions (DECISION_DEFINITION):
 * - READ: Required for retrieving definitions, diagrams, XML and metadata.
 * - CREATE_INSTANCE: Required for evaluating decisions.
 * - UPDATE: Required for modifying the decision definition (e.g., history TTL).
 * - READ_HISTORY: Required for accessing historic decision instances.
 * - DELETE_HISTORY: Required for deleting historic decision instances.
 *
 */

@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController("WebclientDecisionService") @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/decision")
public class DecisionService extends BaseService implements InitializingBean {
	
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else throw new SystemException("DecisionService expects a BpmProvider");
	}
	
	@Operation(
			summary = "Queries for decision definitions that fulfill given parameters. "
					+ "Parameters may be the properties of decision definitions, such as the name, key or version.",
			description = "<strong>Return: Collection of decisions")
	@ApiResponse(responseCode = "400", description = "There is at least one invalid parameter value")
	@GetMapping
	public Collection<Decision> getList(@RequestParam Map<String, Object> queryParams, Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getDecisionDefinitionList(queryParams, user);
	}
	
	@GetMapping("/count")
	public Long getDecisionDefinitionListCount(@RequestParam Map<String, Object> queryParams, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getDecisionDefinitionListCount(queryParams, user);
	}

	@GetMapping("/key/{key}")
	public Decision getDecisionDefinitionByKey(@PathVariable String key, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getDecisionDefinitionByKey(key, user);
	}

	@GetMapping("/key/{key}/diagram")
	public Object getDiagramByKey(@PathVariable String key, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getDiagramByKey(key, user);
	}

	@PostMapping("/key/{key}/evaluate")
	public Object evaluateDecisionDefinitionByKey(@RequestBody Map<String, Object> data, @PathVariable String key, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.CREATE_INSTANCE_ALL);
		return bpmProvider.evaluateDecisionDefinitionByKey(data, key, user);
	}

	@PutMapping("/key/{key}/history-ttl")
	public ResponseEntity<Void> updateHistoryTTLByKey(@RequestBody Map<String, Object> data, @PathVariable String key, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.UPDATE_ALL);
		bpmProvider.updateHistoryTTLByKey(data, key, user);
	    // return 204 No Content, no body
	    return ResponseEntity.noContent().build();
	}

	@GetMapping("/key/{key}/tenant/{tenant}")
	public Decision getDecisionDefinitionByKeyAndTenant(@PathVariable String key, @PathVariable String tenant, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getDecisionDefinitionByKeyAndTenant(key, tenant, user);
	}

	@GetMapping("/key/{key}/tenant/{tenant}/diagram")
	public Object getDiagramByKeyAndTenant(@PathVariable String key, @PathVariable String tenant, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getDiagramByKeyAndTenant(key, tenant, user);
	}

	@PostMapping("/key/{key}/tenant/{tenant}/evaluate")
	public Object evaluateDecisionDefinitionByKeyAndTenant(@RequestBody Map<String, Object> data, @PathVariable String key, @PathVariable String tenant, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.CREATE_INSTANCE_ALL);
		return bpmProvider.evaluateDecisionDefinitionByKeyAndTenant(data, key, tenant, user);
	}

	@PutMapping("/key/{key}/tenant/{tenant}/history-ttl")
	public Object updateHistoryTTLByKeyAndTenant(@PathVariable String key, @PathVariable String tenant, CIBUser user) {
		return bpmProvider.updateHistoryTTLByKeyAndTenant(key, tenant, user);
	}

	@GetMapping("/key/{key}/xml")
	public Object getXmlByKey(@PathVariable String key, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getXmlByKey(key, user);
	}

	@GetMapping("/key/{key}/tenant/{tenant}/xml")
	public Object getXmlByKeyAndTenant(@PathVariable String key, @PathVariable String tenant, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getXmlByKeyAndTenant(key, tenant, user);
	}

	@GetMapping("/id/{id}")
	public Decision getDecisionDefinitionById(@PathVariable String id, @RequestParam Optional<Boolean> extraInfo, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getDecisionDefinitionById(id, extraInfo, user);
	}

	@GetMapping("/id/{id}/diagram")
	public Object getDiagramById(@PathVariable String id, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getDiagramById(id, user);
	}

	@PostMapping("/id/{id}/evaluate")
	public Object evaluateDecisionDefinitionById(@PathVariable String id, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.CREATE_INSTANCE_ALL);
		return bpmProvider.evaluateDecisionDefinitionById(id, user);
	}

	@PutMapping("/id/{id}/history-ttl")
	public ResponseEntity<Void> updateHistoryTTLById(@PathVariable String id, 
			@RequestBody Map<String, Object> data, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.UPDATE_ALL);
		bpmProvider.updateHistoryTTLById(id, data, user);
	    // return 204 No Content, no body
	    return ResponseEntity.noContent().build();
	}

	@GetMapping("/id/{id}/xml")
	public Object getXmlById(@PathVariable String id, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getXmlById(id, user);
	}
	
	@GetMapping("/key/{key}/versions")
	public Collection<Decision> getDecisionVersionsByKey(
			@Parameter(description = "Decision definition key") @PathVariable String key,
			@RequestParam Optional<Boolean> lazyLoad, Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getDecisionVersionsByKey(key, lazyLoad, user);
	}
	
	@Operation(summary = "Get a list of historic decision instances")
	@GetMapping("/history/instances")
	public Collection<HistoricDecisionInstance> getHistoricDecisionInstances(@RequestParam Map<String, Object> queryParams, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_HISTORY_ALL);
		return bpmProvider.getHistoricDecisionInstances(queryParams, user);
	}
	
	@Operation(summary = "Get the count of historic decision instances")
	@GetMapping("/history/instances/count")
	public Long getHistoricDecisionInstanceCount(@RequestParam Map<String, Object> queryParams, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_HISTORY_ALL);
		return bpmProvider.getHistoricDecisionInstanceCount(queryParams, user);
	}

	@Operation(summary = "Get a single historic decision instance by ID")
	@GetMapping("/history/instances/{id}")
	public HistoricDecisionInstance getHistoricDecisionInstanceById(
	        @PathVariable String id,
	        @RequestParam Map<String, Object> queryParams, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.READ_HISTORY_ALL);
		return bpmProvider.getHistoricDecisionInstanceById(id, queryParams, user);
	}

	@Operation(summary = "Delete historic decision instances asynchronously")
	@PostMapping("/history/instances/delete")
	public Object deleteHistoricDecisionInstances(@RequestBody Map<String, Object> body, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.DELETE_HISTORY_ALL);
		return bpmProvider.deleteHistoricDecisionInstances(body, user);
	}

	@Operation(summary = "Set removal time for historic decision instances asynchronously")
	@PostMapping("/history/instances/set-removal-time")
	public Object setHistoricDecisionInstanceRemovalTime(@RequestBody Map<String, Object> body, CIBUser user) {
		checkPermission(user, SevenResourceType.DECISION_DEFINITION, PermissionConstants.UPDATE_ALL);
		return bpmProvider.setHistoricDecisionInstanceRemovalTime(body, user);
	}
	
}