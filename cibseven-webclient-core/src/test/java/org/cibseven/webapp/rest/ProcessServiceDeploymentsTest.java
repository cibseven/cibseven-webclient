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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.Deployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The deployment and variable endpoints of {@link ProcessService}, and in particular how its
 * many optional request parameters are turned into an engine query - a filter that is silently
 * dropped or silently sent as an empty string changes which deployments the user sees.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProcessServiceDeploymentsTest {

	@Mock
	private BpmProvider bpmProvider;

	@Mock
	@SuppressWarnings("rawtypes")
	private BaseUserProvider baseUserProvider;

	private ProcessService processService;
	private HttpServletRequest request;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		processService = new ProcessService();
		ReflectionTestUtils.setField(processService, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(processService, "baseUserProvider", baseUserProvider);
		ReflectionTestUtils.setField(processService, "authorizationEnabled", false);
		user = new CIBUser("demo");
		request = mock(HttpServletRequest.class);
		when(baseUserProvider.checkAuthorization(any(), org.mockito.ArgumentMatchers.anyBoolean()))
			.thenReturn(user);
	}

	/** Runs countDeployments with the given filters and returns the query the provider received. */
	private MultiValueMap<String, String> queryFor(String id, String name, String nameLike, String source,
			Boolean withoutSource, String tenantIdIn, Boolean withoutTenantId,
			Boolean includeWithoutTenantId, String after, String before) {
		// a fresh mock per call, so a test may build more than one query
		org.mockito.Mockito.reset(bpmProvider);
		when(bpmProvider.countDeployments(any(), any())).thenReturn(0L);

		processService.countDeployments(user, id, name, nameLike, source, withoutSource, tenantIdIn,
			withoutTenantId, includeWithoutTenantId, after, before);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<MultiValueMap<String, String>> captor = ArgumentCaptor.forClass(MultiValueMap.class);
		verify(bpmProvider).countDeployments(any(), captor.capture());
		return captor.getValue();
	}

	// ---------- query building ----------

	@Test
	void countDeployments_sendsNoFiltersWhenNoneWereGiven() {
		MultiValueMap<String, String> query =
			queryFor("", "", "", "", false, "", false, false, "", "");

		// empty defaults must not become empty filter values, which the engine would match on
		assertThat(query).isEmpty();
	}

	@Test
	void countDeployments_forwardsEveryTextFilterThatWasGiven() {
		MultiValueMap<String, String> query = queryFor(
			"d-1", "Invoice", "%Invoice%", "cockpit", false, "acme", false, false,
			"2026-01-01T00:00:00.000+0200", "2026-12-31T00:00:00.000+0200");

		assertThat(query.getFirst("id")).isEqualTo("d-1");
		assertThat(query.getFirst("name")).isEqualTo("Invoice");
		assertThat(query.getFirst("nameLike")).isEqualTo("%Invoice%");
		assertThat(query.getFirst("source")).isEqualTo("cockpit");
		assertThat(query.getFirst("tenantIdIn")).isEqualTo("acme");
		assertThat(query.getFirst("after")).isEqualTo("2026-01-01T00:00:00.000+0200");
		assertThat(query.getFirst("before")).isEqualTo("2026-12-31T00:00:00.000+0200");
	}

	@Test
	void countDeployments_sendsTheBooleanFlagsOnlyWhenTheyAreTrue() {
		// the engine rejects "false" for these three, so they have to be omitted rather than sent
		assertThat(queryFor("", "", "", "", false, "", false, false, "", ""))
			.doesNotContainKeys("withoutSource", "withoutTenantId", "includeDeploymentsWithoutTenantId");

		MultiValueMap<String, String> onlyTrue =
			queryFor("", "", "", "", true, "", true, true, "", "");
		assertThat(onlyTrue.getFirst("withoutSource")).isEqualTo("true");
		assertThat(onlyTrue.getFirst("withoutTenantId")).isEqualTo("true");
		assertThat(onlyTrue.getFirst("includeDeploymentsWithoutTenantId")).isEqualTo("true");
	}

	@Test
	void countDeployments_toleratesNullsForEveryFilter() {
		MultiValueMap<String, String> query =
			queryFor(null, null, null, null, null, null, null, null, null, null);

		assertThat(query).isEmpty();
	}

	@Test
	void countDeployments_returnsTheProvidersCount() {
		when(bpmProvider.countDeployments(any(), any())).thenReturn(24L);

		assertThat(processService.countDeployments(user, "", "", "", "", false, "", false, false, "", ""))
			.isEqualTo(24L);
	}

	// ---------- deployment listing ----------

	@Test
	void findDeployments_forwardsThePagingAndSorting() {
		when(bpmProvider.findDeployments(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
			org.mockito.ArgumentMatchers.anyInt(), any(), any())).thenReturn(List.of());

		processService.findDeployments(user, "", "", "", "", false, "", false, false, "", "",
			10, 25, "name", "asc");

		verify(bpmProvider).findDeployments(any(), any(), org.mockito.ArgumentMatchers.eq(10),
			org.mockito.ArgumentMatchers.eq(25), org.mockito.ArgumentMatchers.eq("name"),
			org.mockito.ArgumentMatchers.eq("asc"));
	}

	@Test
	void findDeployment_delegatesToTheProvider() {
		Deployment deployment = new Deployment();
		deployment.setId("d-1");
		when(bpmProvider.findDeployment("d-1", user)).thenReturn(deployment);

		assertThat(processService.findDeployment("d-1", user).getId()).isEqualTo("d-1");
	}

	@Test
	void findDeploymentResources_delegatesToTheProvider() {
		when(bpmProvider.findDeploymentResources("d-1", user)).thenReturn(List.of());

		processService.findDeploymentResources("d-1", user);

		verify(bpmProvider).findDeploymentResources("d-1", user);
	}

	@Test
	void deleteDeployment_answers204AndForwardsCascade() {
		ResponseEntity<Void> response =
			processService.deleteDeployment("d-1", Boolean.TRUE, Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(bpmProvider).deleteDeployment("d-1", Boolean.TRUE, user);
	}

	@Test
	void checkDeployBpmn_authenticatesAndAlwaysAnswersTrue() {
		assertThat(processService.checkDeployBpmn(request)).isTrue();

		verify(baseUserProvider).checkAuthorization(request, true);
	}

	@Test
	void deployBpmn_resolvesTheUserFromTheRequest() {
		org.springframework.util.MultiValueMap<String, Object> data =
			new org.springframework.util.LinkedMultiValueMap<>();
		org.springframework.util.MultiValueMap<String, org.springframework.web.multipart.MultipartFile> file =
			new org.springframework.util.LinkedMultiValueMap<>();

		processService.deployBpmn(data, file, request);

		verify(bpmProvider).deployBpmn(data, file, user);
	}

	// ---------- instances and activities ----------

	@Test
	void findCurrentProcessesInstances_resolvesTheUserFromTheRequest() {
		Map<String, Object> data = new HashMap<>();
		when(bpmProvider.findCurrentProcessesInstances(any(), any(), any(), any())).thenReturn(List.of());

		processService.findCurrentProcessesInstances(
			Optional.of(0), Optional.of(50), data, Locale.ENGLISH, request);

		verify(bpmProvider).findCurrentProcessesInstances(data, Optional.of(0), Optional.of(50), user);
	}

	@Test
	void findProcessesInstances_delegatesToTheProvider() {
		when(bpmProvider.findProcessesInstances("invoice", user)).thenReturn(List.of());

		processService.findProcessesInstances("invoice", Locale.ENGLISH, user);

		verify(bpmProvider).findProcessesInstances("invoice", user);
	}

	@Test
	void findActivityInstance_delegatesToTheProvider() {
		processService.findActivityInstance("pi-1", Locale.ENGLISH, user);

		verify(bpmProvider).findActivityInstance("pi-1", user);
	}

	@Test
	void findCalledProcessDefinitions_delegatesToTheProvider() {
		when(bpmProvider.findCalledProcessDefinitions("id-1", user)).thenReturn(List.of());

		processService.findCalledProcessDefinitions("id-1", Locale.ENGLISH, user);

		verify(bpmProvider).findCalledProcessDefinitions("id-1", user);
	}

	@Test
	void fetchIncidents_delegatesToTheProvider() {
		when(bpmProvider.fetchIncidents("invoice", user)).thenReturn(List.of());

		processService.fetchIncidents("invoice", Locale.ENGLISH, user);

		verify(bpmProvider).fetchIncidents("invoice", user);
	}

	@Test
	void correlateMessage_resolvesTheUserFromTheRequest() {
		Map<String, Object> data = new HashMap<>();
		when(bpmProvider.correlateMessage(data, user)).thenReturn(List.of());

		processService.correlateMessage(data, Locale.ENGLISH, request);

		verify(bpmProvider).correlateMessage(data, user);
	}

	// ---------- process instance variables ----------

	@Test
	void fetchProcessInstanceVariables_appliesTheDocumentedDefaults() {
		when(bpmProvider.fetchProcessInstanceVariables(any(), any(), any())).thenReturn(List.of());

		processService.fetchProcessInstanceVariables("pi-1", Optional.empty(), Optional.empty(),
			Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Locale.ENGLISH, user);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(bpmProvider).fetchProcessInstanceVariables(
			org.mockito.ArgumentMatchers.eq("pi-1"), captor.capture(), org.mockito.ArgumentMatchers.eq(user));
		Map<String, Object> data = captor.getValue();
		// the ignore-case flags default off, deserialization defaults on
		assertThat(data).containsEntry("variableNamesIgnoreCase", Boolean.FALSE)
			.containsEntry("variableValuesIgnoreCase", Boolean.FALSE)
			.containsEntry("deserializeValues", Boolean.TRUE);
		assertThat(data.get("variableName")).isNull();
	}

	@Test
	void fetchProcessInstanceVariables_forwardsTheFiltersThatWereGiven() {
		when(bpmProvider.fetchProcessInstanceVariables(any(), any(), any())).thenReturn(List.of());

		processService.fetchProcessInstanceVariables("pi-1", Optional.of("amount"),
			Optional.of("amo%"), Optional.of("42"), Optional.of(true), Optional.of(true),
			Optional.of(false), Locale.ENGLISH, user);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(bpmProvider).fetchProcessInstanceVariables(
			org.mockito.ArgumentMatchers.eq("pi-1"), captor.capture(), org.mockito.ArgumentMatchers.eq(user));
		assertThat(captor.getValue())
			.containsEntry("variableName", "amount")
			.containsEntry("variableNameLike", "amo%")
			.containsEntry("variableValues", "42")
			.containsEntry("deserializeValues", Boolean.FALSE);
	}

	@Test
	void deleteVariableByExecutionId_answers204() {
		ResponseEntity<Void> response =
			processService.deleteVariableByExecutionId("exec-1", "amount", Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(bpmProvider).deleteVariableByExecutionId("exec-1", "amount", user);
	}
}
