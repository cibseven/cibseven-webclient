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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.BpmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * CIB7-2757: the variable writes of bpm-sdk's task and process-instance resources, sent as the sdk
 * sends them (verb, path, body) through the dispatcher. TaskService is registered alongside, so a
 * collision with the middleware's own task paths would show here.
 */
@ExtendWith(MockitoExtension.class)
class EmbeddedFormVariableWritesRoutingTest {

	private static final String BASE = "/services/v1";

	@Mock
	private BpmProvider bpmProvider;

	private final CIBUser user = new CIBUser("demo");
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		HandlerMethodArgumentResolver userResolver = new HandlerMethodArgumentResolver() {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return CIBUser.class.isAssignableFrom(parameter.getParameterType());
			}

			@Override
			public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
					NativeWebRequest request, WebDataBinderFactory binderFactory) {
				return user;
			}
		};

		mvc = MockMvcBuilders.standaloneSetup(
					withProvider(new EmbeddedFormTaskService()),
					withProvider(new EmbeddedFormProcessInstanceService()),
					withProvider(new TaskService()))
				.setCustomArgumentResolvers(userResolver)
				.addPlaceholderValue("cibseven.webclient.services.basePath", BASE)
				.build();
	}

	private <S extends BaseService> S withProvider(S service) {
		ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);
		return service;
	}

	@Test
	void taskModifyVariables_setsAndRemovesTaskLocalVariables() throws Exception {
		// task.modifyVariables: POST task/{id}/localVariables with its whole params object
		mvc.perform(post(BASE + "/task/task-1/localVariables")
						.contentType("application/json")
						.content("""
							{"id":"task-1",
							 "modifications":{"approved":{"value":true,"type":"Boolean"}},
							 "deletions":["outdated"]}"""))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(bpmProvider).modifyLocalVariables("task-1", Map.of(
				"id", "task-1",
				"modifications", Map.of("approved", Map.of("value", true, "type", "Boolean")),
				"deletions", List.of("outdated")), user);
	}

	@Test
	void taskLocalVariable_setsOneTaskLocalVariable() throws Exception {
		// task.localVariable: PUT task/{id}/localVariables/{varId}, with id and varId in the body
		// a template variable, so MockMvc sends the name encoded once (my%20decision), as the sdk does
		mvc.perform(put(BASE + "/task/task-1/localVariables/{name}", "my decision")
						.contentType("application/json")
						.content("{\"id\":\"task-1\",\"varId\":\"my decision\",\"value\":\"accept\",\"type\":\"String\"}"))
				.andExpect(status().isNoContent());

		verify(bpmProvider).putLocalVariable("task-1", "my decision",
				Map.of("id", "task-1", "varId", "my decision", "value", "accept", "type", "String"), user);
	}

	@Test
	void taskDeleteVariable_removesTheTaskLocalVariable() throws Exception {
		// task.deleteVariable: DELETE task/{id}/localVariables/{escaped varId}
		mvc.perform(delete(BASE + "/task/task-1/localVariables/{name}", "my decision"))
				.andExpect(status().isNoContent());

		verify(bpmProvider).deleteLocalVariable("task-1", "my decision", user);
	}

	@Test
	void processInstanceSetVariable_setsAProcessVariable() throws Exception {
		// process-instance.setVariable: PUT process-instance/{id}/variables/{escaped name}, name in the body
		mvc.perform(put(BASE + "/process-instance/pi-1/variables/customer")
						.contentType("application/json")
						.content("{\"name\":\"customer\",\"value\":\"ACME\",\"type\":\"String\"}"))
				.andExpect(status().isNoContent());

		verify(bpmProvider).putProcessInstanceVariable("pi-1", "customer",
				Map.of("name", "customer", "value", "ACME", "type", "String"), user);
	}

	@Test
	void readingTheTaskLocalVariablesStillWorks() throws Exception {
		when(bpmProvider.findLocalVariables("task-1", user)).thenReturn(Map.of("approved", Map.of("value", true)));

		mvc.perform(get(BASE + "/task/task-1/localVariables"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.approved.value").value(true));
	}

	@Test
	void otherProcessInstancePathsAreNotServed() throws Exception {
		// only setVariable: the batch and async operations of the sdk resource stay unmapped
		mvc.perform(post(BASE + "/process-instance/variables-async").contentType("application/json").content("{}"))
				.andExpect(status().isNotFound());
		mvc.perform(get(BASE + "/process-instance/pi-1/variables/customer"))
				.andExpect(status().isMethodNotAllowed());
		verify(bpmProvider, never()).putProcessInstanceVariable(any(), any(), any(), any());
	}
}
