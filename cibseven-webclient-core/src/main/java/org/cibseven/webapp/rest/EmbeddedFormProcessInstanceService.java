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

import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.config.SevenWebclientProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Engine-rest compatible process instance paths consumed by embedded forms.
 *
 * <p>Forms are rendered by {@code bpm-sdk}, whose process-instance resource builds engine-native
 * paths. Only setting a process variable is served here; the batch and async operations of that
 * resource are not meant for forms.</p>
 */
@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController
@RequestMapping(SevenWebclientProperties.SERVICES_BASE_PATH + "/process-instance")
public class EmbeddedFormProcessInstanceService extends BaseService {

	@Operation(
			summary = "Set a variable of a process instance (engine-rest compatible path used by embedded forms)",
			description = "Body as engine-rest: value, type and valueInfo")
	@PutMapping("/{processInstanceId}/variables/{variableName}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void putVariable(
			@Parameter(description = "Process instance Id") @PathVariable String processInstanceId,
			@Parameter(description = "Variable name") @PathVariable String variableName,
			@Parameter(description = "The variable value") @RequestBody Map<String, Object> variable,
			CIBUser user) {
		bpmProvider.putProcessInstanceVariable(processInstanceId, variableName, variable, user);
	}
}
