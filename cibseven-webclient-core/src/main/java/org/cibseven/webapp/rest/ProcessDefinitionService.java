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
import org.cibseven.webapp.config.SevenWebclientProperties;
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
@RestController @RequestMapping(SevenWebclientProperties.SERVICES_BASE_PATH + "/process-definition")
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
		return EngineResponses.relay(bpmProvider.getRenderedStartForm(processDefinitionId, params, user));
	}

	/*
	 * Kept as it is for compatibility: without variableNames the path segment is a definition key,
	 * with variableNames it is a definition id. The bpm-sdk (embedded and generated start forms)
	 * sends "{id}/form-variables?variableNames=..." and relies on the id reading (see #1402).
	 * Callers that have a key and want to filter by name use "/key/{key}/form-variables".
	 */
	@Operation(
			summary = "Get form variables for process definition",
			description = "<strong>Return: Map of form variable names to Variable objects. Without variableNames the path segment "
					+ "is the process definition key, with variableNames it is the process definition id. "
					+ "Use /key/{key}/form-variables to filter by name for a key.</strong>")
	@ApiResponse(responseCode = "404", description = "Process definition or form variables not found")
	@RequestMapping(value = "/{key}/form-variables", method = RequestMethod.GET)
	public Map<String, Variable> fetchProcessDefinitionFormVariablesByNames(
			@PathVariable String key,
			@RequestParam(required = false, defaultValue = "true") boolean deserializeValues,
			@RequestParam(required = false) String variableNames,
			CIBUser user) {
		List<String> variableListName = parseVariableNames(variableNames);
		if (variableListName != null) {
			return bpmProvider.fetchProcessFormVariables(variableListName, key, deserializeValues, user);
		} else {
			return bpmProvider.fetchProcessFormVariables(key, deserializeValues, user);
		}
	}

	@Operation(
			summary = "Get form variables for process definition by key",
			description = "<strong>Return: Map of form variable names to Variable objects for the latest version of the process "
					+ "definition with the given key and no tenant id. Variables can optionally be deserialized and filtered by name.</strong>")
	@ApiResponse(responseCode = "404", description = "Process definition or form variables not found")
	@RequestMapping(value = "/key/{key}/form-variables", method = RequestMethod.GET)
	public Map<String, Variable> fetchProcessDefinitionFormVariablesByKey(
			@PathVariable String key,
			@RequestParam(required = false, defaultValue = "true") boolean deserializeValues,
			@RequestParam(required = false) String variableNames,
			CIBUser user) {
		List<String> variableListName = parseVariableNames(variableNames);
		if (variableListName != null) {
			return bpmProvider.fetchProcessFormVariablesByKey(variableListName, key, deserializeValues, user);
		} else {
			return bpmProvider.fetchProcessFormVariables(key, deserializeValues, user);
		}
	}

	private static List<String> parseVariableNames(String variableNames) {
		if (variableNames == null || variableNames.isEmpty()) {
			return null;
		}
		return List.of(variableNames.split(","));
	}

	@Consumes(MediaType.APPLICATION_JSON)
	@RequestMapping(value = "/{processDefinitionId}/submit-form", method = RequestMethod.POST)
	public ResponseEntity<ProcessStart> submitForm(
			@PathVariable String processDefinitionId,
			@RequestBody String formResult,
			HttpServletRequest rq, CIBUser user) {
		ProcessStart processStart = bpmProvider.submitForm(processDefinitionId, formResult, user);
		return ResponseEntity.ok(processStart);
	}
}