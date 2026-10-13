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

import org.cibseven.webapp.auth.CIBUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Engine-rest compatible task paths consumed by embedded forms.
 *
 * <p>Forms are rendered by {@code bpm-sdk}, whose task resource builds engine-native paths. The
 * ones a form reaches for beyond reading and submitting are served here; what the middleware
 * already offers under its own names, such as the form and the identity links, stays where it is.</p>
 *
 * <p>Unclaiming is the engine's assignee reset, so it goes through the same provider call the
 * curated endpoint uses rather than a second way of doing it.</p>
 */
@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/task")
public class EmbeddedFormTaskService extends BaseService {

	@Operation(
			summary = "Claim a task for a user (engine-rest compatible path used by embedded forms)",
			description = "Refused by the engine when the task is already held by someone else")
	@PostMapping("/{taskId}/claim")
	public void claim(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			@Parameter(description = "The user taking the task") @RequestBody Map<String, String> body,
			CIBUser user) {
		bpmProvider.claim(taskId, body.get("userId"), user);
	}

	@Operation(
			summary = "Unclaim a task (engine-rest compatible path used by embedded forms)",
			description = "Leaves the task without an assignee")
	@PostMapping("/{taskId}/unclaim")
	public void unclaim(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			CIBUser user) {
		bpmProvider.setAssignee(taskId, "null", user);
	}

	@Operation(
			summary = "Delegate a task to a user (engine-rest compatible path used by embedded forms)",
			description = "The task returns to its owner when the delegate resolves it")
	@PostMapping("/{taskId}/delegate")
	public void delegate(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			@Parameter(description = "The user the task is delegated to") @RequestBody Map<String, String> body,
			CIBUser user) {
		bpmProvider.delegate(taskId, body.get("userId"), user);
	}

	@Operation(
			summary = "Get the local variables of a task (engine-rest compatible path used by embedded forms)",
			description = "<strong>Return: the variables held by the task itself, by name")
	@GetMapping("/{taskId}/localVariables")
	public Map<String, Object> localVariables(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			CIBUser user) {
		return bpmProvider.findLocalVariables(taskId, user);
	}

	@Operation(
			summary = "Get the comments on a task (engine-rest compatible path used by embedded forms)",
			description = "<strong>Return: the comments the engine holds for the task")
	@GetMapping("/{taskId}/comment")
	public Collection<Map<String, Object>> comments(
			@Parameter(description = "Task Id") @PathVariable String taskId,
			CIBUser user) {
		return bpmProvider.findComments(taskId, user);
	}
}
