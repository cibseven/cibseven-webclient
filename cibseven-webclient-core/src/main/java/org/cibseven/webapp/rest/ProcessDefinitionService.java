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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.StartForm;
import org.cibseven.webapp.rest.model.Variable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;

@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occurred"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/process-definition")
public class ProcessDefinitionService extends BaseService implements InitializingBean {
	

	@Override
	public void afterPropertiesSet() {
	}

    @Operation(
			summary = "Get start-form to start a process",
			description = "<strong>Return: Startform variables and formReference")
	@ApiResponse(responseCode = "404", description = "Process not found")
	@RequestMapping(value = "/{processDefinitionId}/startForm", method = RequestMethod.GET)
	public StartForm fetchStartForm(
			@Parameter(description = "Process to be started") @PathVariable String processDefinitionId,
			Locale loc, HttpServletRequest request, CIBUser user) {
		checkPermission(user, SevenResourceType.PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.fetchStartForm(processDefinitionId, user);
	}

	@Operation(
			summary = "Get rendered form HTML for process definition",
			description = "<strong>Return: Rendered form HTML as string</strong>")
	@ApiResponse(responseCode = "404", description= "Process definition or rendered form not found")
	@RequestMapping(value = "/{processDefinitionId}/rendered-form", method = RequestMethod.GET, produces = "text/html")
	public ResponseEntity<String> getRenderedStartForm(
			@Parameter(description = "Process definition Id") @PathVariable String processDefinitionId,
			@RequestParam Map<String, Object> params,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		return bpmProvider.getRenderedStartForm(processDefinitionId, params, user);
	}

	@Operation(
			summary = "Get form variables for process definition",
			description = "<strong>Return: Map of form variable names to Variable objects for the given process definition key. "
					+ "Variables can optionally be deserialized and filtered by name.</strong>")
	@ApiResponse(responseCode = "404", description = "Process definition or form variables not found")
	@RequestMapping(value = "/{key}/form-variables", method = RequestMethod.GET)
	public Map<String, Variable> fetchProcessDefinitionFormVariablesByNames(
			@PathVariable String key,
			@RequestParam(required = false, defaultValue = "true") boolean deserializeValues,
			@RequestParam(required = false) String variableNames,
			CIBUser user) {
		checkPermission(user, SevenResourceType.PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		List<String> variableListName = null;
		if (variableNames != null && !variableNames.isEmpty()) {
			variableListName = List.of(variableNames.split(","));
		}
		if (variableListName != null) {
			return bpmProvider.fetchProcessFormVariables(variableListName, key, deserializeValues, user);
		} else {
			return bpmProvider.fetchProcessFormVariables(key, deserializeValues, user);
		}
	}

	@Consumes(MediaType.APPLICATION_JSON)
	@RequestMapping(value = "/{processDefinitionKey}/submit-form", method = RequestMethod.POST)
	public ResponseEntity<ProcessStart> submitForm(
			@PathVariable String processDefinitionKey, 
			@RequestBody String formResult, 
			HttpServletRequest rq, CIBUser user) {
		checkPermission(user, SevenResourceType.PROCESS_INSTANCE, PermissionConstants.CREATE_ALL);
		ProcessStart processStart = bpmProvider.submitForm(processDefinitionKey, formResult, user);
		return new ResponseEntity<>(processStart, new HttpHeaders(), HttpStatus.OK);
	}
}