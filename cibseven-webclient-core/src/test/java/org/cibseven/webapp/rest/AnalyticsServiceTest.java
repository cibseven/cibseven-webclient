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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.IProcessProvider;
import org.cibseven.webapp.rest.model.Analytics;
import org.cibseven.webapp.rest.model.AnalyticsInfo;
import org.cibseven.webapp.rest.model.Decision;
import org.cibseven.webapp.rest.model.IncidentInfo;
import org.cibseven.webapp.rest.model.ProcessDefinitionInfo;
import org.cibseven.webapp.rest.model.ProcessStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Replaces {@code AnalyticsServiceIT}, which drove a real engine over HTTP and asserted a seeded
 * demo dataset (7 running instances, "Invoice Receipt", 24 deployments) that no longer exists.
 * That class is excluded from the failsafe run - see the root pom - and these tests cover the
 * same aggregation logic against mocked providers instead.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AnalyticsServiceTest {

	@Mock
	private BpmProvider bpmProvider;

	@Mock
	private IProcessProvider processProvider;

	private AnalyticsService analyticsService;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		analyticsService = new AnalyticsService();
		ReflectionTestUtils.setField(analyticsService, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(analyticsService, "processProvider", processProvider);
		ReflectionTestUtils.setField(analyticsService, "authorizationEnabled", false);
		user = new CIBUser("demo");

		// sensible empty defaults; each test overrides what it cares about
		when(bpmProvider.getProcessStatistics(anyMap(), any())).thenReturn(List.of());
		when(processProvider.groupProcessStatisticsByKeyAndTenant(any())).thenReturn(List.of());
		when(bpmProvider.findTasksCount(anyMap(), any())).thenReturn(0);
		when(bpmProvider.getDecisionDefinitionList(anyMap(), any())).thenReturn(List.of());
		when(bpmProvider.countDeployments(any(), any())).thenReturn(0L);
		when(bpmProvider.getRuntimeBatchCount(anyMap(), any())).thenReturn(0L);
	}

	private ProcessStatistics statistics(String key, String name, String tenantId, long instances,
			Long incidentCount) {
		ProcessDefinitionInfo definition = new ProcessDefinitionInfo();
		definition.setKey(key);
		definition.setName(name);
		definition.setTenantId(tenantId);

		ProcessStatistics statistics = new ProcessStatistics();
		statistics.setDefinition(definition);
		statistics.setInstances(instances);
		if (incidentCount != null) {
			IncidentInfo incident = new IncidentInfo();
			incident.setIncidentType("failedJob");
			incident.setIncidentCount(incidentCount);
			statistics.setIncidents(List.of(incident));
		}
		return statistics;
	}

	@Test
	void afterPropertiesSet_doesNothingButMustNotThrow() {
		analyticsService.afterPropertiesSet();
	}

	@Test
	void getAnalytics_reportsRunningInstancesPerDefinition() {
		when(processProvider.groupProcessStatisticsByKeyAndTenant(any()))
			.thenReturn(List.of(statistics("invoice", "Invoice Receipt", null, 7L, null)));

		Analytics analytics = analyticsService.getAnalytics(Locale.ENGLISH, user);

		assertThat(analytics.getRunningInstances()).singleElement()
			.extracting(AnalyticsInfo::getId, AnalyticsInfo::getTitle, AnalyticsInfo::getValue)
			.containsExactly("invoice", "Invoice Receipt", 7L);
		assertThat(analytics.getProcessDefinitionsCount()).isEqualTo(1);
	}

	@Test
	void getAnalytics_qualifiesTheIdWithTheTenantWhenThereIsOne() {
		when(processProvider.groupProcessStatisticsByKeyAndTenant(any()))
			.thenReturn(List.of(statistics("invoice", "Invoice Receipt", "acme", 3L, null)));

		Analytics analytics = analyticsService.getAnalytics(Locale.ENGLISH, user);

		// two tenants can deploy the same key, so the chart id has to carry the tenant
		assertThat(analytics.getRunningInstances()).singleElement()
			.extracting(AnalyticsInfo::getId).isEqualTo("invoice:acme");
	}

	@Test
	void getAnalytics_reportsOpenIncidents() {
		when(processProvider.groupProcessStatisticsByKeyAndTenant(any()))
			.thenReturn(List.of(statistics("invoice", "Invoice Receipt", null, 7L, 2L)));

		Analytics analytics = analyticsService.getAnalytics(Locale.ENGLISH, user);

		assertThat(analytics.getOpenIncidents()).singleElement()
			.extracting(AnalyticsInfo::getValue).isEqualTo(2L);
	}

	@Test
	void getAnalytics_omitsDefinitionsWhoseIncidentCountIsZero() {
		when(processProvider.groupProcessStatisticsByKeyAndTenant(any()))
			.thenReturn(List.of(statistics("invoice", "Invoice Receipt", null, 7L, 0L)));

		Analytics analytics = analyticsService.getAnalytics(Locale.ENGLISH, user);

		// a zero row would draw an empty slice in the incident chart
		assertThat(analytics.getOpenIncidents()).isEmpty();
	}

	@Test
	void getAnalytics_omitsDefinitionsWithNoIncidentDataAtAll() {
		when(processProvider.groupProcessStatisticsByKeyAndTenant(any()))
			.thenReturn(List.of(statistics("invoice", "Invoice Receipt", null, 7L, null)));

		assertThat(analyticsService.getAnalytics(Locale.ENGLISH, user).getOpenIncidents()).isEmpty();
	}

	@Test
	void getAnalytics_sortsRunningInstancesByCountDescending() {
		when(processProvider.groupProcessStatisticsByKeyAndTenant(any())).thenReturn(List.of(
			statistics("small", "Small", null, 1L, null),
			statistics("big", "Big", null, 9L, null),
			statistics("medium", "Medium", null, 5L, null)));

		Analytics analytics = analyticsService.getAnalytics(Locale.ENGLISH, user);

		assertThat(analytics.getRunningInstances()).extracting(AnalyticsInfo::getId)
			.containsExactly("big", "medium", "small");
	}

	@Test
	void getAnalytics_collapsesTheTailIntoAnOthersGroupBeyondTwentyDefinitions() {
		List<ProcessStatistics> many = new ArrayList<>(IntStream.rangeClosed(1, 25)
			.mapToObj(i -> statistics("key" + i, "Process " + i, null, i, null))
			.toList());
		when(processProvider.groupProcessStatisticsByKeyAndTenant(any())).thenReturn(many);

		Analytics analytics = analyticsService.getAnalytics(Locale.ENGLISH, user);

		// 19 real rows plus one "others" row, so the chart stays readable
		assertThat(analytics.getRunningInstances()).hasSize(AnalyticsService.MAX_ANALYTICS_GROUPS);
		AnalyticsInfo others = analytics.getRunningInstances()
			.get(analytics.getRunningInstances().size() - 1);
		assertThat(others.getTitle()).isEqualTo("others");
		assertThat(others.getId()).isNull();
		// it keeps the top MAX_ANALYTICS_GROUPS-1 = 19 rows (counts 25..7) and sums the
		// remaining six (6+5+4+3+2+1) into "others"
		assertThat(others.getValue()).isEqualTo(IntStream.rangeClosed(1, 6).sum());
	}

	@Test
	void getAnalytics_reportsTheThreeHumanTaskBuckets() {
		when(bpmProvider.findTasksCount(anyMap(), any())).thenReturn(4, 2, 1);

		Analytics analytics = analyticsService.getAnalytics(Locale.ENGLISH, user);

		assertThat(analytics.getOpenHumanTasks())
			.extracting(AnalyticsInfo::getTitle, AnalyticsInfo::getValue)
			.containsExactly(
				org.assertj.core.api.Assertions.tuple("assigned", 4L),
				org.assertj.core.api.Assertions.tuple("assignedGroups", 2L),
				org.assertj.core.api.Assertions.tuple("unassigned", 1L));
	}

	@Test
	void getAnalytics_countsDistinctDecisionKeys() {
		Decision first = new Decision();
		first.setKey("risk");
		Decision sameKeyNewVersion = new Decision();
		sameKeyNewVersion.setKey("risk");
		Decision other = new Decision();
		other.setKey("pricing");
		when(bpmProvider.getDecisionDefinitionList(anyMap(), any()))
			.thenReturn(List.of(first, sameKeyNewVersion, other));

		Analytics analytics = analyticsService.getAnalytics(Locale.ENGLISH, user);

		// versions of the same decision must not be counted twice
		assertThat(analytics.getDecisionDefinitionsCount()).isEqualTo(2);
	}

	@Test
	void getAnalytics_reportsMinusOneWhenTheDecisionListIsUnavailable() {
		when(bpmProvider.getDecisionDefinitionList(anyMap(), any())).thenReturn(null);

		assertThat(analyticsService.getAnalytics(Locale.ENGLISH, user).getDecisionDefinitionsCount())
			.isEqualTo(-1);
	}

	@Test
	void getAnalytics_reportsMinusOneWhenTheDeploymentCountIsUnavailable() {
		when(bpmProvider.countDeployments(any(), any())).thenReturn(null);

		assertThat(analyticsService.getAnalytics(Locale.ENGLISH, user).getDeploymentsCount())
			.isEqualTo(-1);
	}

	@Test
	void getAnalytics_reportsTheDeploymentAndBatchCounts() {
		when(bpmProvider.countDeployments(any(), any())).thenReturn(24L);
		when(bpmProvider.getRuntimeBatchCount(anyMap(), any())).thenReturn(3L);

		Analytics analytics = analyticsService.getAnalytics(Locale.ENGLISH, user);

		assertThat(analytics.getDeploymentsCount()).isEqualTo(24L);
		assertThat(analytics.getBatchesCount()).isEqualTo(3L);
	}

	@Test
	void getAnalytics_treatsAnUnavailableBatchCountAsZero() {
		when(bpmProvider.getRuntimeBatchCount(anyMap(), any())).thenReturn(null);

		// unlike decisions and deployments, a missing batch count reports 0 rather than -1
		assertThat(analyticsService.getAnalytics(Locale.ENGLISH, user).getBatchesCount()).isZero();
	}

	@Test
	void getAnalytics_returnsEmptyChartsWhenTheEngineHasNothing() {
		Analytics analytics = analyticsService.getAnalytics(Locale.ENGLISH, user);

		assertThat(analytics.getRunningInstances()).isEmpty();
		assertThat(analytics.getOpenIncidents()).isEmpty();
		assertThat(analytics.getProcessDefinitionsCount()).isZero();
	}

	@Test
	void getAnalytics_asksForFailedJobsAndRootIncidents() {
		analyticsService.getAnalytics(Locale.ENGLISH, user);

		org.mockito.ArgumentCaptor<java.util.Map<String, Object>> captor =
			org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
		org.mockito.Mockito.verify(bpmProvider).getProcessStatistics(captor.capture(), any());
		assertThat(captor.getValue())
			.containsEntry("failedJobs", true)
			.containsEntry("rootIncidents", true);
	}
}
