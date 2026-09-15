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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.Process;
import org.cibseven.webapp.rest.model.ProcessDiagram;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link ProcessService} is a thin REST facade: it runs the deprecated permission check and then
 * delegates to the {@link BpmProvider}. These tests pin that contract - which resource type and
 * permission each endpoint demands, and what it hands on - without standing up a web context.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProcessServiceTest {

	@Mock
	private BpmProvider bpmProvider;

	private ProcessService processService;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		processService = new ProcessService();
		ReflectionTestUtils.setField(processService, "bpmProvider", bpmProvider);
		// the deprecated in-webclient permission check is off by default
		ReflectionTestUtils.setField(processService, "authorizationEnabled", false);
		user = new CIBUser("demo");
	}

	@Test
	void afterPropertiesSet_doesNothingButMustNotThrow() {
		processService.afterPropertiesSet();
	}

	// ---------- delegation ----------

	@Test
	void findProcesses_returnsWhatTheProviderReturns() {
		Process process = new Process();
		process.setKey("invoice");
		when(bpmProvider.findProcesses(user)).thenReturn(List.of(process));

		assertThat(processService.findProcesses(Locale.ENGLISH, user))
			.singleElement().extracting(Process::getKey).isEqualTo("invoice");
	}

	@Test
	void findProcessesWithInfo_delegatesToTheExtraInfoQuery() {
		when(bpmProvider.findProcessesWithInfo(user)).thenReturn(List.of());

		processService.findProcessesWithInfo(Locale.ENGLISH, user);

		verify(bpmProvider).findProcessesWithInfo(user);
		verify(bpmProvider, never()).findProcesses(any());
	}

	@Test
	void findProcessesWithFilters_passesAnEmptyStringWhenNoFilterIsSubmitted() {
		when(bpmProvider.findProcessesWithFilters("", user)).thenReturn(List.of());

		processService.findProcessesWithFilters(Optional.empty(), Locale.ENGLISH, user);

		// the provider builds a query string, so it must never receive null
		verify(bpmProvider).findProcessesWithFilters("", user);
	}

	@Test
	void findProcessesWithFilters_forwardsTheSubmittedFilter() {
		when(bpmProvider.findProcessesWithFilters("name=Invoice", user)).thenReturn(List.of());

		processService.findProcessesWithFilters(Optional.of("name=Invoice"), Locale.ENGLISH, user);

		verify(bpmProvider).findProcessesWithFilters("name=Invoice", user);
	}

	@Test
	void findProcessVersionsByDefinitionKey_forwardsTheTenantAndLazyLoadFlag() {
		when(bpmProvider.findProcessVersionsByDefinitionKey("invoice", "acme", Optional.of(true), user))
			.thenReturn(List.of());

		processService.findProcessVersionsByDefinitionKey(
			"invoice", "acme", Optional.of(true), Locale.ENGLISH, user);

		verify(bpmProvider).findProcessVersionsByDefinitionKey("invoice", "acme", Optional.of(true), user);
	}

	@Test
	void fetchDiagram_delegatesToTheProvider() {
		ProcessDiagram diagram = new ProcessDiagram("id-1", "<definitions/>");
		when(bpmProvider.fetchDiagram("id-1", user)).thenReturn(diagram);

		assertThat(processService.fetchDiagram("id-1", Locale.ENGLISH, user).getBpmn20Xml())
			.isEqualTo("<definitions/>");
	}

	@Test
	void findProcessStatistics_delegatesToTheProvider() {
		when(bpmProvider.findProcessStatistics("id-1", user)).thenReturn(List.of());

		processService.findProcessStatistics("id-1", Locale.ENGLISH, user);

		verify(bpmProvider).findProcessStatistics("id-1", user);
	}

	@Test
	void getProcessStatistics_forwardsTheQueryParameters() {
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("failedJobs", "true");
		when(bpmProvider.getProcessStatistics(queryParams, user)).thenReturn(List.of());

		processService.getProcessStatistics(queryParams, Locale.ENGLISH, user);

		verify(bpmProvider).getProcessStatistics(queryParams, user);
	}

	// ---------- responses with no body ----------

	@Test
	void suspendProcessInstance_answers204() {
		ResponseEntity<Void> response =
			processService.suspendProcessInstance("pi-1", Boolean.TRUE, Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(response.getBody()).isNull();
		verify(bpmProvider).suspendProcessInstance("pi-1", Boolean.TRUE, user);
	}

	@Test
	void deleteProcessInstance_answers204() {
		ResponseEntity<Void> response =
			processService.deleteProcessInstance("pi-1", Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(bpmProvider).deleteProcessInstance("pi-1", user);
	}

	@Test
	void deleteProcessDefinition_answers204AndForwardsCascade() {
		ResponseEntity<Void> response =
			processService.deleteProcessDefinition("id-1", Optional.of(true), Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(bpmProvider).deleteProcessDefinition("id-1", Optional.of(true), user);
	}

	@Test
	void suspendProcessDefinition_answers204AndForwardsEveryArgument() {
		ResponseEntity<Void> response = processService.suspendProcessDefinition(
			"id-1", Boolean.TRUE, Boolean.FALSE, Optional.of("2026-01-01T10:00:00"), Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(bpmProvider).suspendProcessDefinition(
			"id-1", Boolean.TRUE, Boolean.FALSE, "2026-01-01T10:00:00", user);
	}

	@Test
	void suspendProcessDefinition_unwrapsAnAbsentExecutionDateToNull() {
		processService.suspendProcessDefinition(
			"id-1", Boolean.TRUE, Boolean.FALSE, Optional.empty(), Locale.ENGLISH, user);

		// null means "suspend now"; see ProcessProviderTest for what a non-null date does to the body
		verify(bpmProvider).suspendProcessDefinition("id-1", Boolean.TRUE, Boolean.FALSE, null, user);
	}

	// ---------- the deprecated permission check ----------

	@Test
	void permissionCheck_isSkippedWhileLegacyAuthorizationIsDisabled() {
		when(bpmProvider.findProcesses(user)).thenReturn(List.of());

		processService.findProcesses(Locale.ENGLISH, user);

		// the engine is authoritative; the webclient must not ask for authorizations at all
		verify(bpmProvider, never()).getUserAuthorization(any());
	}

	@Test
	void permissionCheck_consultsTheEngineWhenLegacyAuthorizationIsEnabled() {
		ReflectionTestUtils.setField(processService, "authorizationEnabled", true);
		when(bpmProvider.getUserAuthorization(user)).thenReturn(authorizationsGranting("ALL"));
		when(bpmProvider.findProcesses(user)).thenReturn(List.of());

		processService.findProcesses(Locale.ENGLISH, user);

		verify(bpmProvider).getUserAuthorization(user);
	}

	@Test
	void permissionCheck_refusesWhenTheUserLacksThePermission() {
		ReflectionTestUtils.setField(processService, "authorizationEnabled", true);
		when(bpmProvider.getUserAuthorization(user)).thenReturn(authorizationsGranting("NOTHING_USEFUL"));

		assertThatThrownBy(() -> processService.findProcesses(Locale.ENGLISH, user))
			.isInstanceOf(AccessDeniedException.class);
		verify(bpmProvider, never()).findProcesses(any());
	}

	/** A grant on PROCESS_DEFINITION carrying exactly the given permission. */
	private Authorizations authorizationsGranting(String permission) {
		Authorization grant = new Authorization();
		grant.setType(1); // AUTH_TYPE_GRANT
		grant.setPermissions(new String[] { permission });
		grant.setResourceType(SevenResourceType.PROCESS_DEFINITION.getType());
		grant.setResourceId("*");
		Authorizations authorizations = new Authorizations();
		authorizations.setProcessDefinition(List.of(grant));
		return authorizations;
	}
}
