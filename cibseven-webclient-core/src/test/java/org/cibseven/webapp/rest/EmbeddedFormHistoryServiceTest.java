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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.TaskHistory;
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
 * The engine-rest compatible historic task query used by embedded forms. bpm-sdk sends the filters
 * in the request body via POST, with pagination in the query string, so the verb and the body
 * handling are pinned through the real dispatcher: a handler mapped to the wrong verb is invisible
 * to tests that call the controller method directly.
 */
@ExtendWith(MockitoExtension.class)
public class EmbeddedFormHistoryServiceTest {

	private static final String BASE = "/services/v1";
	private static final String PATH = BASE + "/history/task";

	@Mock
	private BpmProvider bpmProvider;

	private MockMvc mvc;

	@BeforeEach
	public void setUp() {
		CIBUser user = mock(CIBUser.class);
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

		EmbeddedFormHistoryService service = new EmbeddedFormHistoryService();
		ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);
		// checkPermission short-circuits when authorization is disabled, keeping these tests on
		// the routing and payload handling rather than the authorization chain.
		ReflectionTestUtils.setField(service, "authorizationEnabled", false);

		mvc = MockMvcBuilders.standaloneSetup(service)
				.setCustomArgumentResolvers(userResolver)
				.addPlaceholderValue("cibseven.webclient.services.basePath", BASE)
				.build();
	}

	@Test
	public void queryIsServedWithPostTheVerbTheSdkUses() throws Exception {
		when(bpmProvider.findHistoryTasks(any(), any(), any(), any()))
				.thenReturn(List.of(mock(TaskHistory.class)));

		mvc.perform(post(PATH).contentType("application/json")
						.content("{\"processInstanceId\":\"pi-1\"}"))
				.andExpect(status().isOk());
	}

	@Test
	public void queryRejectsAGetSoTheSdkVerbCannotRegress() throws Exception {
		mvc.perform(get(PATH)).andExpect(status().isMethodNotAllowed());
	}

	@Test
	public void everyBodyFilterIsForwardedToTheProvider() throws Exception {
		when(bpmProvider.findHistoryTasks(any(), any(), any(), any())).thenReturn(List.of());

		mvc.perform(post(PATH).contentType("application/json")
						.content("{\"processInstanceId\":\"pi-1\",\"taskAssignee\":\"demo\"}"))
				.andExpect(status().isOk());

		// the whole filter map is passed through, not just a curated subset
		verify(bpmProvider).findHistoryTasks(
				eq(Map.of("processInstanceId", "pi-1", "taskAssignee", "demo")),
				eq(Optional.empty()), eq(Optional.empty()), any());
	}

	@Test
	public void paginationIsTakenFromTheQueryStringNotTheBody() throws Exception {
		when(bpmProvider.findHistoryTasks(any(), any(), any(), any())).thenReturn(List.of());

		mvc.perform(post(PATH).param("firstResult", "10").param("maxResults", "25")
						.contentType("application/json").content("{\"processInstanceId\":\"pi-1\"}"))
				.andExpect(status().isOk());

		verify(bpmProvider).findHistoryTasks(eq(Map.of("processInstanceId", "pi-1")),
				eq(Optional.of(10)), eq(Optional.of(25)), any());
	}

	@Test
	public void anAbsentBodyIsTreatedAsNoFilters() throws Exception {
		when(bpmProvider.findHistoryTasks(any(), any(), any(), any())).thenReturn(List.of());

		mvc.perform(post(PATH).contentType("application/json")).andExpect(status().isOk());

		verify(bpmProvider).findHistoryTasks(eq(Map.of()), eq(Optional.empty()), eq(Optional.empty()), any());
	}

	@Test
	public void countIsServedWithPostAndTheEngineCountShape() throws Exception {
		when(bpmProvider.findHistoryTasksCount(any(), any())).thenReturn(4);

		mvc.perform(post(PATH + "/count").contentType("application/json")
						.content("{\"processInstanceId\":\"pi-1\"}"))
				.andExpect(status().isOk())
				// bpm-sdk expects the engine's object form, not a bare number
				.andExpect(jsonPath("$.count").value(4));
	}

	@Test
	public void countForwardsTheFilters() throws Exception {
		when(bpmProvider.findHistoryTasksCount(any(), any())).thenReturn(0);

		mvc.perform(post(PATH + "/count").contentType("application/json")
						.content("{\"taskAssignee\":\"demo\"}"))
				.andExpect(status().isOk());

		verify(bpmProvider).findHistoryTasksCount(eq(Map.of("taskAssignee", "demo")), any());
	}

	@Test
	public void countTreatsAnAbsentBodyAsNoFilters() throws Exception {
		when(bpmProvider.findHistoryTasksCount(any(), any())).thenReturn(0);

		mvc.perform(post(PATH + "/count").contentType("application/json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(0));

		verify(bpmProvider).findHistoryTasksCount(eq(Map.of()), any());
	}

}
