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

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.rest.model.TaskHistory;
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
 * Engine-rest compatible historic task instance query.
 *
 * <p>Two consumers rely on these paths. Embedded forms are rendered by {@code bpm-sdk}, whose
 * client is pointed at the middleware rather than at the engine, so the engine-native
 * {@code history/task} path has to resolve here; the curated equivalents live under the
 * {@code /task-history/} prefix with a different shape, which the sdk does not know about. The
 * enterprise historic task search UI calls the same paths.</p>
 *
 * <p>The count is returned in the engine's {@code {"count": n}} form rather than as a bare
 * number, so that a form written against engine-rest behaves the same way here.</p>
 */
@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/history/task")
public class EmbeddedFormHistoryService extends BaseService {

	@Operation(
			summary = "Query historic task instances (engine-rest compatible path used by embedded forms)",
			description = "Filters are taken from the request body and forwarded to the engine's historic task query." + "<br>"
			+ "<strong>Return: Collection of historic task instances")
	@PostMapping("")
	public Collection<TaskHistory> queryHistoricTasks(
			@Parameter(description = "Filters to apply to the historic task query") @RequestBody(required = false) Map<String, Object> filters,
			@Parameter(description = "Index of the first result to return") @RequestParam Optional<Integer> firstResult,
			@Parameter(description = "Maximum number of results to return") @RequestParam Optional<Integer> maxResults,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.HISTORIC_TASK, PermissionConstants.READ_ALL);
		return bpmProvider.findHistoryTasks(filters == null ? Map.of() : filters, firstResult, maxResults, user);
	}

	@Operation(
			summary = "Count historic task instances (engine-rest compatible path used by embedded forms)",
			description = "<strong>Return: JSON object with a single count property")
	@PostMapping("/count")
	public Map<String, Integer> queryHistoricTaskCount(
			@Parameter(description = "Filters to apply to the historic task query") @RequestBody(required = false) Map<String, Object> filters,
			Locale loc, CIBUser user) {
		checkPermission(user, SevenResourceType.HISTORIC_TASK, PermissionConstants.READ_ALL);
		return Map.of("count", bpmProvider.findHistoryTasksCount(filters == null ? Map.of() : filters, user));
	}
}
