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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.IdentityLink;
import org.cibseven.webapp.rest.model.Task;
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

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TaskServiceTest {

	@Mock
	private BpmProvider bpmProvider;

	@Mock
	@SuppressWarnings("rawtypes")
	private org.cibseven.webapp.auth.BaseUserProvider baseUserProvider;

	private TaskService taskService;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		taskService = new TaskService();
		ReflectionTestUtils.setField(taskService, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(taskService, "baseUserProvider", baseUserProvider);
		ReflectionTestUtils.setField(taskService, "authorizationEnabled", false);
		user = new CIBUser("demo");
	}

	@Test
	void afterPropertiesSet_doesNothingButMustNotThrow() {
		taskService.afterPropertiesSet();
	}

	// ---------- lookup ----------

	@Test
	void findTaskById_delegatesToTheProvider() {
		Task task = new Task();
		task.setId("task-1");
		when(bpmProvider.findTaskById("task-1", user)).thenReturn(task);

		assertThat(taskService.findTaskById("task-1", Locale.ENGLISH, user).getId()).isEqualTo("task-1");
	}

	@Test
	void findTasksCount_forwardsTheFilterBody() {
		Map<String, Object> filters = new HashMap<>();
		filters.put("unfinished", true);
		when(bpmProvider.findTasksCount(filters, user)).thenReturn(5);

		assertThat(taskService.findTasksCount(filters, Locale.ENGLISH, user)).isEqualTo(5);
	}

	@Test
	void findTasksByProcessInstance_delegatesToTheProvider() {
		when(bpmProvider.findTasksByProcessInstance("pi-1", user)).thenReturn(List.of());

		taskService.findTasksByProcessInstance("pi-1", Locale.ENGLISH, user);

		verify(bpmProvider).findTasksByProcessInstance("pi-1", user);
	}

	@Test
	void findTasksPost_resolvesTheUserFromTheRequestAndForwardsTheFilterBody() {
		// this endpoint takes the raw request and authenticates it itself, unlike its siblings
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(baseUserProvider.checkAuthorization(request, true)).thenReturn(user);
		Map<String, Object> data = new HashMap<>();
		when(bpmProvider.findTasksPost(data, user)).thenReturn(List.of());

		taskService.findTasksPost(data, Locale.ENGLISH, request);

		verify(baseUserProvider).checkAuthorization(request, true);
		verify(bpmProvider).findTasksPost(data, user);
	}

	// ---------- mutation, all answering 204 ----------

	@Test
	void submit_withoutABodyAnswers204() {
		ResponseEntity<Void> response = taskService.submit("task-1", Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(bpmProvider).submit("task-1", user);
	}

	@Test
	void submit_withAFormResultForwardsItVerbatim() {
		ResponseEntity<Void> response =
			taskService.submit("task-1", "{\"variables\":{}}", Locale.ENGLISH, null, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(bpmProvider).submit("task-1", "{\"variables\":{}}", user);
	}

	@Test
	void setAssignee_answers204() {
		ResponseEntity<Void> response = taskService.setAssignee("task-1", "demo", Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(bpmProvider).setAssignee("task-1", "demo", user);
	}

	@Test
	void update_answers204() {
		Task task = new Task();
		task.setId("task-1");

		ResponseEntity<Void> response = taskService.update(task, Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(bpmProvider).update(task, user);
	}

	// ---------- identity links ----------

	@Test
	void findIdentityLink_forwardsTheOptionalType() {
		when(bpmProvider.findIdentityLink("task-1", Optional.of("candidate"), user)).thenReturn(List.of());

		taskService.findIdentityLink("task-1", Optional.of("candidate"), Locale.ENGLISH, user);

		verify(bpmProvider).findIdentityLink("task-1", Optional.of("candidate"), user);
	}

	@Test
	void createIdentityLink_answers204() {
		Map<String, Object> data = new HashMap<>();
		data.put("userId", "demo");

		ResponseEntity<Void> response =
			taskService.createIdentityLink("task-1", data, Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(bpmProvider).createIdentityLink("task-1", data, user);
	}

	@Test
	void deleteIdentityLink_answers204() {
		Map<String, Object> data = new HashMap<>();
		data.put("userId", "demo");

		ResponseEntity<Void> response =
			taskService.deleteIdentityLink("task-1", data, Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(bpmProvider).deleteIdentityLink("task-1", data, user);
	}

	// ---------- forms ----------

	@Test
	void form_delegatesToTheProvider() {
		when(bpmProvider.form("task-1", user)).thenReturn("empty-task");

		assertThat(taskService.form("task-1", Locale.ENGLISH, user)).isEqualTo("empty-task");
	}

	@Test
	void formReference_delegatesToTheProvider() {
		when(bpmProvider.formReference("task-1", user)).thenReturn("my-form");

		assertThat(taskService.formReference("task-1", Locale.ENGLISH, user)).isEqualTo("my-form");
	}

	@Test
	void getDeployedForm_passesTheFormThroughWithItsHeaders() {
		byte[] form = "<form/>".getBytes(StandardCharsets.UTF_8);
		when(bpmProvider.getDeployedForm("task-1", user))
			.thenReturn(ResponseEntity.ok().header("Content-Type", "application/xhtml+xml").body(form));

		ResponseEntity<byte[]> response = taskService.getDeployedForm("task-1", Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(form);
		assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("application/xhtml+xml");
	}

	@Test
	void getDeployedForm_answers204WhenTheFormIsEmpty() {
		when(bpmProvider.getDeployedForm("task-1", user)).thenReturn(ResponseEntity.ok(new byte[0]));

		ResponseEntity<byte[]> response = taskService.getDeployedForm("task-1", Locale.ENGLISH, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test
	void getDeployedForm_answers204WhenThereIsNoBody() {
		when(bpmProvider.getDeployedForm("task-1", user)).thenReturn(ResponseEntity.ok().build());

		assertThat(taskService.getDeployedForm("task-1", Locale.ENGLISH, user).getStatusCode())
			.isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test
	void getDeployedForm_wrapsAProviderFailure() {
		when(bpmProvider.getDeployedForm("task-1", user)).thenThrow(new IllegalStateException("boom"));

		assertThatThrownBy(() -> taskService.getDeployedForm("task-1", Locale.ENGLISH, user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Error getting deployed form");
	}

	// ---------- form variables ----------

	@Test
	void fetchFormVariables_withoutNamesAsksForAllOfThem() throws Exception {
		when(bpmProvider.fetchFormVariables("task-1", true, user)).thenReturn(Map.of());

		taskService.fetchFormVariables("task-1", true, null, user);

		verify(bpmProvider).fetchFormVariables("task-1", true, user);
		verify(bpmProvider, never()).fetchFormVariables(anyList(), anyString(), anyBoolean(), any());
	}

	@Test
	void fetchFormVariables_treatsAnEmptyNameListAsNoFilter() throws Exception {
		when(bpmProvider.fetchFormVariables("task-1", true, user)).thenReturn(Map.of());

		taskService.fetchFormVariables("task-1", true, "", user);

		verify(bpmProvider).fetchFormVariables("task-1", true, user);
	}

	@Test
	void fetchFormVariables_splitsTheCommaSeparatedNames() throws Exception {
		when(bpmProvider.fetchFormVariables(List.of("amount", "customer"), "task-1", false, user))
			.thenReturn(Map.of());

		taskService.fetchFormVariables("task-1", false, "amount,customer", user);

		verify(bpmProvider).fetchFormVariables(List.of("amount", "customer"), "task-1", false, user);
	}

	// ---------- activity variables ----------

	@Test
	void fetchActivityVariables_delegatesToTheProvider() {
		when(bpmProvider.fetchActivityVariables("activity-1", user)).thenReturn(List.of());

		taskService.fetchActivityVariables("activity-1", Locale.ENGLISH, user);

		verify(bpmProvider).fetchActivityVariables("activity-1", user);
	}

	@Test
	void findIdentityLink_returnsWhatTheProviderReturns() {
		IdentityLink link = new IdentityLink("demo", null, "assignee");
		when(bpmProvider.findIdentityLink("task-1", Optional.empty(), user)).thenReturn(List.of(link));

		assertThat(taskService.findIdentityLink("task-1", Optional.empty(), Locale.ENGLISH, user))
			.singleElement().extracting(IdentityLink::getUserId).isEqualTo("demo");
	}
}
