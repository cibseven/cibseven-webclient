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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.Authorizations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins path and verb of the task bpmnError/bpmnEscalation endpoints through the real dispatcher.
 * A handler mapped to the wrong verb or path still passes tests that call the method directly.
 */
class TaskBpmnEscalationRoutingTest {

	private static final CIBUser USER = new CIBUser("demo");

	private BpmProvider bpmProvider;
	private TaskService taskService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		bpmProvider = mock(BpmProvider.class);
		taskService = new TaskService();
		ReflectionTestUtils.setField(taskService, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(taskService, "authorizationEnabled", false);
		mockMvc = MockMvcBuilders.standaloneSetup(taskService)
			.setCustomArgumentResolvers(new CibUserArgumentResolver())
			.addPlaceholderValue("cibseven.webclient.services.basePath", "/services/v1")
			.build();
	}

	@Test
	void postBpmnEscalationIsMappedAndForwardsCodeAndVariables() throws Exception {
		mockMvc.perform(post("/services/v1/task/task-1/bpmnEscalation")
				.contentType("application/json")
				.content("{\"escalationCode\":\"esc-1\",\"variables\":{\"amount\":{\"value\":42,\"type\":\"Integer\"}}}"))
			.andExpect(status().isNoContent());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(bpmProvider).handleBpmnEscalation(eq("task-1"), captor.capture(), eq(USER));
		assertEquals("esc-1", captor.getValue().get("escalationCode"));
		assertEquals(Map.of("amount", Map.of("value", 42, "type", "Integer")), captor.getValue().get("variables"));
	}

	@Test
	void postBpmnEscalationWorksWithoutVariables() throws Exception {
		mockMvc.perform(post("/services/v1/task/task-1/bpmnEscalation")
				.contentType("application/json")
				.content("{\"escalationCode\":\"esc-1\"}"))
			.andExpect(status().isNoContent());

		verify(bpmProvider).handleBpmnEscalation(eq("task-1"), any(), eq(USER));
	}

	@Test
	void getBpmnEscalationIsNotAllowed() throws Exception {
		mockMvc.perform(get("/services/v1/task/task-1/bpmnEscalation"))
			.andExpect(status().isMethodNotAllowed());

		verify(bpmProvider, never()).handleBpmnEscalation(any(), any(), any());
	}

	@Test
	void postBpmnErrorIsStillMapped() throws Exception {
		mockMvc.perform(post("/services/v1/task/task-1/bpmnError")
				.contentType("application/json")
				.content("{\"errorCode\":\"err-1\",\"errorMessage\":\"boom\"}"))
			.andExpect(status().isNoContent());

		verify(bpmProvider).handleBpmnError(eq("task-1"), any(), eq(USER));
	}

	@Test
	void escalationRequiresTaskUpdatePermissionLikeBpmnError() {
		ReflectionTestUtils.setField(taskService, "authorizationEnabled", true);
		Authorizations noRights = new Authorizations();
		noRights.setTask(List.of());
		when(bpmProvider.getUserAuthorization(USER)).thenReturn(noRights);

		assertThrows(AccessDeniedException.class, () -> taskService.handleBpmnEscalation("task-1",
			Map.<String, Object>of("escalationCode", "esc-1"), Optional.<String>empty(), USER));
		assertThrows(AccessDeniedException.class, () -> taskService.handleBpmnError("task-1",
			Map.<String, Object>of("errorCode", "err-1"), Optional.<String>empty(), USER));

		verify(bpmProvider, never()).handleBpmnEscalation(any(), any(), any());
	}

	private static class CibUserArgumentResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return CIBUser.class.equals(parameter.getParameterType());
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
			return USER;
		}
	}
}
