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
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.springframework.web.bind.annotation.GetMapping;
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
 * Engine-rest compatible history paths consumed by embedded forms.
 *
 * <p>Forms are rendered by {@code bpm-sdk}, whose client is pointed at the middleware rather than
 * at the engine, so the paths its history resource builds have to resolve here. The curated
 * equivalents live under {@code /process-history/}, which the sdk does not know about.</p>
 *
 * <p>A <em>path alias only</em>: every call delegates to {@link HistoryProcessService}, so the
 * permission handling and the provider calls stay single-sourced there. The verbs mirror the sdk,
 * which posts the process instance query and sends the activity instance query as parameters, and
 * the count answers in the engine's {@code count} object rather than as a bare number.</p>
 */
@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}")
public class EmbeddedFormHistoryQueryService extends BaseService {

	private final HistoryProcessService historyProcessService;

	public EmbeddedFormHistoryQueryService(HistoryProcessService historyProcessService) {
		this.historyProcessService = historyProcessService;
	}

	@Operation(
			summary = "Query historic process instances (engine-rest compatible path used by embedded forms)",
			description = "<strong>Return: Collection of historic process instances")
	@PostMapping("/history/process-instance")
	public Collection<HistoryProcessInstance> queryHistoricProcessInstances(
			@Parameter(description = "Filters to apply to the historic process instance query")
			@RequestBody(required = false) Map<String, Object> filters,
			@Parameter(description = "Index of the first result to return") @RequestParam Optional<Integer> firstResult,
			@Parameter(description = "Maximum number of results to return") @RequestParam Optional<Integer> maxResults,
			CIBUser user) {
		return historyProcessService.findProcessesInstancesHistory(filters == null ? Map.of() : filters,
				firstResult, maxResults, user);
	}

	@Operation(
			summary = "Count historic process instances (engine-rest compatible path used by embedded forms)",
			description = "<strong>Return: JSON object with a single count property")
	@PostMapping("/history/process-instance/count")
	public Map<String, Long> queryHistoricProcessInstanceCount(
			@Parameter(description = "Filters to apply to the historic process instance query")
			@RequestBody(required = false) Map<String, Object> filters,
			CIBUser user) {
		return Map.of("count",
				historyProcessService.countProcessesInstancesHistory(filters == null ? Map.of() : filters, user));
	}

	@Operation(
			summary = "Query historic activity instances (engine-rest compatible path used by embedded forms)",
			description = "The sdk sends this query as request parameters." + "<br>"
			+ "<strong>Return: Collection of historic activity instances")
	@GetMapping("/history/activity-instance")
	public Collection<ActivityInstanceHistory> queryHistoricActivityInstances(
			@Parameter(description = "Filters to apply to the historic activity instance query")
			@RequestParam Map<String, Object> queryParams,
			Locale loc, CIBUser user) {
		return historyProcessService.findActivitiesInstancesHistory(queryParams, user);
	}
}
