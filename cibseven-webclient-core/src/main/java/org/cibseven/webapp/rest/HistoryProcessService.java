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

import jakarta.servlet.http.HttpServletRequest;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}")
public class HistoryProcessService extends BaseService {

	/*
	@RequestMapping(value = "/instance-history/by-process-instance/{processInstanceId}", method = RequestMethod.GET)
	public ProcessInstance findHistoryProcessInstanceHistory(@PathVariable String processInstanceId, Locale loc, CIBSevenUser user) {
		return bpmProvider.findHistoryProcessInstanceHistory(processInstanceId, user);
	}
	*/

	@Operation(
			summary = "Queries for historic process instances that fulfill the given parameters",
			description = "Parameters firstResult and maxResults are used for pagination")
	@ApiResponse(responseCode = "400", description = "There is at least one invalid parameter value")
	@RequestMapping(value = "/process-history/instance", method = RequestMethod.POST)
	public Collection<HistoryProcessInstance> findProcessesInstancesHistory(
			@Parameter(description = "Parameters to filter query") @RequestBody Map<String, Object> filters,
			@Parameter(description = "Index of the first result to return") @RequestParam Optional<Integer> firstResult,
			@Parameter(description = "Maximum number of results to return") @RequestParam Optional<Integer> maxResults,
			CIBUser user) {
		checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.findProcessesInstancesHistory(filters, firstResult, maxResults, user);
	}
	
	@Operation(
	    summary = "Count historic process instances that fulfill the given parameters",
	    description = "Returns the total number of matching historic process instances"
	)
	@ApiResponse(responseCode = "400", description = "There is at least one invalid parameter value")
	@RequestMapping(value = "/process-history/instance/count", method = RequestMethod.POST)
	public Long countProcessesInstancesHistory(
	        @Parameter(description = "Parameters to filter query") @RequestBody Map<String, Object> filters,
	        CIBUser user) {
	    checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
	    return bpmProvider.countProcessesInstancesHistory(filters, user);
	}	
	
	@Operation(
			summary = "Get processes instances with a specific process key (in the history)",
			description = "Parameters firstResult and maxResults are used for pagination")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/process-history/instance/by-process-key/{key}", method = RequestMethod.GET)
	public Collection<HistoryProcessInstance> findProcessesInstancesHistory(
			@Parameter(description = "Process key") @PathVariable String key, 
			@Parameter(description = "True means that unfinished processes will be fetched<br>False only finished processes will be fetched") @RequestParam Optional<Boolean> active, 
			@Parameter(description = "Index of the first result to return") @RequestParam Integer firstResult,
			@Parameter(description = "Maximum number of results to return") @RequestParam Integer maxResults,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.findProcessesInstancesHistory(key, active, firstResult, maxResults, user);
	}
	
	@Operation(
			summary = "Get processes instances with a specific process id (in the history)",
			description = "Parameters firstResult and maxResults are used for pagination<br>Parameters text and activityId are used for filtering")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/process-history/instance/by-process-id/{id}", method = RequestMethod.GET)
	public Collection<HistoryProcessInstance> findProcessesInstancesHistoryById(
			@Parameter(description = "Process Id") @PathVariable String id,
			@Parameter(description = "Process Activity Id") @RequestParam Optional<String> activityId,
			@Parameter(description = "True means that unfinished processes will be fetched<br>False only finished processes will be fetched") @RequestParam Optional<Boolean> active,
			@Parameter(description = "Index of the first result to return") @RequestParam Integer firstResult,
			@Parameter(description = "Maximum number of results to return") @RequestParam Integer maxResults,
			@Parameter(description = "Filter by text") @RequestParam String text,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.findProcessesInstancesHistoryById(id, activityId, active, firstResult, maxResults, text, user);
	}	
	
	@Operation(
			summary = "Get variables from a specific process instance",
			description = "The variables found belongs to the history, they have other attributes and variables from finished process instances are also fetched")
	@ApiResponse(responseCode = "404", description = "Variable not found")
	@RequestMapping(value = "/process-history/instance/by-process-instance/{processInstanceId}/variables", method = RequestMethod.GET)
	public Collection<VariableHistory> fetchProcessInstanceVariablesHistory(
			@Parameter(description = "Filter by process instance Id") @PathVariable String processInstanceId,
			@Parameter(description = "Deserialize value") @RequestParam Optional<Boolean> deserialize,
			Locale loc, CIBUser user) {
        checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.fetchProcessInstanceVariablesHistory(processInstanceId, user, deserialize);
	}
	
	// Used for chat-comments, to find parent of a process instanceId
	@ApiResponse(responseCode = "404", description = "Process instance not found")
	@RequestMapping(value = "/process-history/instance/{processInstanceId}", method = RequestMethod.GET)
	public HistoryProcessInstance findProcessInstance(
			@Parameter(description = "Process instance Id") @PathVariable String processInstanceId,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.findHistoryProcessInstanceHistory(processInstanceId, user);
	}
	
	@ApiResponse(responseCode = "404", description = "Process instance not found")
	@RequestMapping(value = "/process-history/instance/{id}", method = RequestMethod.DELETE)
	public void deleteProcessInstanceFromHistory(
			@Parameter(description = "Process instance Id") @PathVariable String id,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteProcessInstanceFromHistory(id, user);
	}
	
	@Operation(summary = "Delete a variable in the historic process instance")
	@ApiResponse(responseCode = "404")
	@RequestMapping(value = "/process-history/instance/{id}/variables", method = RequestMethod.DELETE)
	public void deleteVariableHistoryInstance(
			@Parameter(description = "Id of the variable") @PathVariable String id,
			Locale loc, CIBUser user) {
		checkCockpitRights(user);
		checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteVariableHistoryInstance(id, user);
	}
	
	@Operation(
			summary = "Queries for historic activity instances that fulfill the given parameters",
			description = "The activities found belongs to the history")
	@ApiResponses({
		@ApiResponse(responseCode = "400", description = "Invalid attribute value exception")
	})
	@RequestMapping(value = "/process-history/activity", method = RequestMethod.GET)
	public Collection<ActivityInstanceHistory> findActivitiesInstancesHistory(@RequestParam Map<String, Object> queryParams, CIBUser user) {
		checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.findActivitiesInstancesHistory(queryParams, user);
	}	
	
	@Operation(
			summary = "Get activities instances that belong to a process instance",
			description = "The activities found belongs to the history, they have other attributes and activities from finished processes are also fetched")
	@ApiResponses({
		@ApiResponse(responseCode = "400", description = "Invalid attribute value exception"),
		@ApiResponse(responseCode = "404", description = "Process instance not found")
	})
	@RequestMapping(value = "/process-history/activity/by-process-instance/{processInstanceId}", method = RequestMethod.GET)
	public Collection<ActivityInstanceHistory> findActivitiesInstancesHistory(
			@Parameter(description = "Filter by process instance Id") @PathVariable String processInstanceId,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.findActivitiesInstancesHistory(processInstanceId, user);
	}	
	
	@Operation(
			summary = "Get activities instances that belong to a process definition",
			description = "The activities found belongs to the history, they have other attributes and activities from finished processes are also fetched")
	@ApiResponses({
		@ApiResponse(responseCode = "400", description = "Invalid attribute value exception"),
		@ApiResponse(responseCode = "404", description = "Process definition not found")
	})
	@RequestMapping(value = "/process-history/activity/by-process-definition/{processDefinitionId}", method = RequestMethod.GET)
	public Collection<ActivityInstanceHistory> findActivitiesProcessDefinitionHistory(
			@Parameter(description = "Filter by process definition Id") @PathVariable String processDefinitionId,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.findActivitiesProcessDefinitionHistory(processDefinitionId, user);
	}	
	
	@Operation(summary = "Get a variable data from the process history")
	@ApiResponse(responseCode = "404", description = "Variable not found")
	@RequestMapping(value = "/process-history/variable/{id}/data", method = RequestMethod.GET)
	public ResponseEntity<byte[]> fetchHistoryVariableDataById(
			@Parameter(description = "Id of the variable") @PathVariable String id,
			Locale loc, CIBUser user) {
        checkPermission(user, SevenResourceType.HISTORIC_PROCESS_INSTANCE, PermissionConstants.READ_ALL);
		return bpmProvider.fetchHistoryVariableDataById(id, user);
	}
}
