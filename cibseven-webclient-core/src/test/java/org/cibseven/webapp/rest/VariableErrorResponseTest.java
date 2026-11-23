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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * The variable endpoints that catch every exception themselves answer an {@code ErrorMessage} with a
 * controlled text, so {@code utils/error.js} can show it, and never the raw engine message (CIB7-2806).
 * Run through the real dispatcher, because the methods are declared as {@code ResponseEntity<String>}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VariableErrorResponseTest {

	private static final String BASE = "/services/v1";
	private static final String SUBMIT = BASE + "/task/task-1/submit-variables?processInstanceId=pi-1&processDefinitionId=pd-1";
	private static final String TECHNICAL = "org.cibseven.bpm.engine.ProcessEngineException: ENGINE-03005 SQL details";

	@Mock
	private BpmProvider bpmProvider;

	@Mock
	@SuppressWarnings("rawtypes")
	private BaseUserProvider baseUserProvider;

	private MockMvc mvc;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		user = new CIBUser("demo");
		when(baseUserProvider.authenticateUser(any())).thenReturn(user);

		TaskService taskService = new TaskService();
		ProcessService processService = new ProcessService();
		for (BaseService service : new BaseService[] { taskService, processService }) {
			ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);
			ReflectionTestUtils.setField(service, "baseUserProvider", baseUserProvider);
		}

		mvc = MockMvcBuilders.standaloneSetup(taskService, processService)
				.addPlaceholderValue("cibseven.webclient.services.basePath", BASE)
				.build();
	}

	private Task taskAssignedTo(String assignee) {
		Task task = new Task();
		task.setId("task-1");
		task.setAssignee(assignee);
		return task;
	}

	// ---------- task submit-variables ----------

	@Test
	void submitVariables_answersOk() throws Exception {
		when(bpmProvider.findTaskById("task-1", user)).thenReturn(taskAssignedTo("demo"));

		mvc.perform(post(SUBMIT + "&close=true").contentType(MediaType.APPLICATION_JSON).content("[]"))
				.andExpect(status().isOk())
				.andExpect(content().string("ok"));

		verify(bpmProvider).submit(any(Task.class), anyList(), eq(user));
	}

	@Test
	void submitVariables_aMissingTaskIsA404WithAReadableText() throws Exception {
		when(bpmProvider.findTaskById("task-1", user)).thenThrow(new NoObjectFoundException(TECHNICAL));

		mvc.perform(post(SUBMIT).contentType(MediaType.APPLICATION_JSON).content("[]"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.type").value("NoObjectFoundException"))
				.andExpect(jsonPath("$.params[0]").value("The task or its process instance no longer exists."))
				.andExpect(content().string(not(containsString("ENGINE-03005"))));
	}

	@Test
	void submitVariables_aTaskOfSomeoneElseIsRejectedWithAReadableText() throws Exception {
		when(bpmProvider.findTaskById("task-1", user)).thenReturn(taskAssignedTo("mary"));

		mvc.perform(post(SUBMIT).contentType(MediaType.APPLICATION_JSON).content("[]"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("AccessDeniedException"))
				.andExpect(jsonPath("$.params[0]").value("The task is not assigned to the user who submits it."));

		verify(bpmProvider, never()).submitVariables(anyString(), anyList(), any(), anyString());
	}

	@Test
	void submitVariables_anUnassignedTaskIsRejectedInsteadOfFailingWithANullPointer() throws Exception {
		when(bpmProvider.findTaskById("task-1", user)).thenReturn(taskAssignedTo(null));

		mvc.perform(post(SUBMIT).contentType(MediaType.APPLICATION_JSON).content("[]"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("AccessDeniedException"));
	}

	@Test
	void submitVariables_anEngineFailureHidesTheTechnicalMessage() throws Exception {
		when(bpmProvider.findTaskById("task-1", user)).thenReturn(taskAssignedTo("demo"));
		doThrow(new SystemException(TECHNICAL)).when(bpmProvider).submitVariables(eq("pi-1"), anyList(), eq(user), eq("pd-1"));

		mvc.perform(post(SUBMIT).contentType(MediaType.APPLICATION_JSON).content("[]"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("SystemException"))
				.andExpect(jsonPath("$.params[0]").value("The form could not be submitted."))
				.andExpect(content().string(not(containsString("ENGINE-03005"))));
	}

	// ---------- process instance variables ----------

	@Test
	void saveVariable_aMissingInstanceIsA404WithAReadableText() throws Exception {
		doThrow(new NoObjectFoundException(TECHNICAL)).when(bpmProvider).saveVariableInProcessInstanceId(eq("pi-1"), anyList(), eq(user));

		mvc.perform(post(BASE + "/process/pi-1/variables").contentType(MediaType.APPLICATION_JSON).content("[]"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.params[0]").value("The process instance no longer exists."));
	}

	@Test
	void saveVariable_anEngineFailureHidesTheTechnicalMessage() throws Exception {
		doThrow(new SystemException(TECHNICAL)).when(bpmProvider).saveVariableInProcessInstanceId(eq("pi-1"), anyList(), eq(user));

		mvc.perform(post(BASE + "/process/pi-1/variables").contentType(MediaType.APPLICATION_JSON).content("[]"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.params[0]").value("The variables could not be saved."))
				.andExpect(content().string(not(containsString("ENGINE-03005"))));
	}

	@Test
	void fetchVariable_aMissingVariableIsA404WithAReadableText() throws Exception {
		when(bpmProvider.fetchVariableByProcessInstanceId("pi-1", "amount", user)).thenThrow(new NoObjectFoundException(TECHNICAL));

		mvc.perform(get(BASE + "/process/pi-1/variable/amount"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.params[0]").value("The process instance or the variable does not exist."));
	}

	@Test
	void fetchVariable_anEngineFailureHidesTheTechnicalMessage() throws Exception {
		when(bpmProvider.fetchVariableByProcessInstanceId("pi-1", "amount", user)).thenThrow(new SystemException(TECHNICAL));

		mvc.perform(get(BASE + "/process/pi-1/variable/amount"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.params[0]").value("The variable could not be read."))
				.andExpect(content().string(not(containsString("ENGINE-03005"))));
	}
}
