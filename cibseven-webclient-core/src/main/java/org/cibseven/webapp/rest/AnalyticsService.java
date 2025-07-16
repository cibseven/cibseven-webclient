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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.IProcessProvider;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.Analytics;
import org.cibseven.webapp.rest.model.AnalyticsInfo;
import org.cibseven.webapp.rest.model.Decision;
import org.cibseven.webapp.rest.model.ProcessStatistics;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({ @ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
		@ApiResponse(responseCode = "401", description = "Unauthorized") })
@RestController
@RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/analytics")
public class AnalyticsService extends BaseService implements InitializingBean {

	public static final int MAX_ANALYTICS_GROUPS = 20;

	@Autowired
	BpmProvider bpmProvider;
	
	@Autowired
	IProcessProvider processProvider;
	
	SevenProvider sevenProvider;

	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else
			throw new SystemException("AnalyticsService expects a BpmProvider");
	}

	@Operation(summary = "Get analytics for processes, decisions and human Tasks", description = "<strong>Return: Analytics")
	@GetMapping("")
	public Analytics getAnalytics(Locale loc, CIBUser user) {

		checkPermission(user, SevenResourceType.PROCESS_DEFINITION, PermissionConstants.READ_ALL);
		
		Collection<ProcessStatistics> processStatistics = bpmProvider.getProcessStatistics(user);

		Analytics analytics = new Analytics();

		List<AnalyticsInfo> runningInstances = new ArrayList<>();
		List<AnalyticsInfo> openIncidents = new ArrayList<>();

		// Group by the key and tenant ID using the shared utility method
		List<ProcessStatistics> groupedStats = processProvider.groupProcessStatisticsByKeyAndTenant(processStatistics);


		groupedStats.forEach(stats -> {
			String key = stats.getDefinition().getKey();
			String title = stats.getDefinition().getName();
			String tenantId = stats.getDefinition().getTenantId();
			String combinedId = key + (tenantId != null ? ":" + tenantId : "");
			
			AnalyticsInfo instancesInfo = new AnalyticsInfo();
			instancesInfo.setId(combinedId);
			instancesInfo.setTitle(title);
			instancesInfo.setValue(stats.getInstances());
			runningInstances.add(instancesInfo);

			if (stats.getIncidents() != null && !stats.getIncidents().isEmpty()
					&& stats.getIncidents().get(0).getIncidentCount() != 0) {
				AnalyticsInfo incidentsInfo = new AnalyticsInfo();
				incidentsInfo.setId(combinedId);
				incidentsInfo.setTitle(title);
				incidentsInfo.setValue(stats.getIncidents().get(0).getIncidentCount());
				openIncidents.add(incidentsInfo);
			}
		});
		analytics.setRunningInstances(groupAnalyticsWithOthers(runningInstances));
		analytics.setOpenIncidents(groupAnalyticsWithOthers(openIncidents));

		Map<String, Object> params = Map.of("unfinished", true, "assigned", true);
		Integer assignedTasks = bpmProvider.findTasksCount(params, user);
		params = Map.of("unfinished", true, "unassigned", true, "withCandidateGroups", true);
		Integer assignedGroups = bpmProvider.findTasksCount(params, user);
		params = Map.of("unfinished", true, "unassigned", true, "withoutCandidateGroups", true);
		Integer unassignedTasks = bpmProvider.findTasksCount(params, user);

		analytics.setOpenHumanTasks(List.of(new AnalyticsInfo("1", "assigned", assignedTasks),
				new AnalyticsInfo("2", "assignedGroups", assignedGroups),
				new AnalyticsInfo("3", "unassigned", unassignedTasks)));

		analytics.setProcessDefinitionsCount(runningInstances.size());
		Collection<Decision> decisionDefinitionList = bpmProvider.getDecisionDefinitionList(new HashMap<>(), user);
		if (decisionDefinitionList == null) {
			analytics.setDecisionDefinitionsCount(-1);
		} else {
			// Count the number of distinct keys
			long distinctKeyCount = decisionDefinitionList.stream().map(Decision::getKey).distinct().count();

			analytics.setDecisionDefinitionsCount(distinctKeyCount);
		}

		Long deploymentsCount = bpmProvider.countDeployments(user, "");
		if (deploymentsCount == null) {
			analytics.setDeploymentsCount(-1);
		} else {
			analytics.setDeploymentsCount(deploymentsCount);
		}

//    ToDo:
//    analytics.setBatchesCount(0);

		return analytics;
	}

	private List<AnalyticsInfo> groupAnalyticsWithOthers(List<AnalyticsInfo> analytics) {
		List<AnalyticsInfo> groupedList = new ArrayList<>();
		if (analytics == null || analytics.isEmpty()) {
			return groupedList;
		}
		
		analytics.sort(Comparator.comparingLong(AnalyticsInfo::getValue).reversed());
		
		if (analytics.size() <= MAX_ANALYTICS_GROUPS) {
			groupedList.addAll(analytics);
		} else {
			groupedList.addAll(analytics.subList(0, MAX_ANALYTICS_GROUPS - 1));
			long othersSum = analytics.subList(MAX_ANALYTICS_GROUPS - 1, analytics.size()).stream()
					.mapToLong(AnalyticsInfo::getValue).sum();

			AnalyticsInfo othersGroup = new AnalyticsInfo();
			othersGroup.setId(null);
			othersGroup.setTitle("others");
			othersGroup.setValue(othersSum);

			groupedList.add(othersGroup);
		}
		return groupedList;
	}

}