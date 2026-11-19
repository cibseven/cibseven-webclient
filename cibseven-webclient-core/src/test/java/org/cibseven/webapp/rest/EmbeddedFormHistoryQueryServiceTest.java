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
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * The engine-rest shaped history paths an embedded form reaches through bpm-sdk. They are aliases,
 * so what is pinned here is the routing: the verb the sdk uses, where it puts the query, and the
 * count shape. What the query means is {@link HistoryProcessService}'s business and is tested there.
 */
@ExtendWith(MockitoExtension.class)
public class EmbeddedFormHistoryQueryServiceTest {

	private static final String BASE = "/services/v1";

	@Mock
	private HistoryProcessService historyProcessService;

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

		mvc = MockMvcBuilders.standaloneSetup(new EmbeddedFormHistoryQueryService(historyProcessService))
				.setCustomArgumentResolvers(userResolver)
				.addPlaceholderValue("cibseven.webclient.services.basePath", BASE)
				.build();
	}

	@Test
	public void theProcessInstanceQueryIsPostedWithTheFiltersInTheBody() throws Exception {
		when(historyProcessService.findProcessesInstancesHistory(any(), any(), any(), any()))
				.thenReturn(List.of(mock(HistoryProcessInstance.class)));

		mvc.perform(post(BASE + "/history/process-instance").contentType("application/json")
						.content("{\"processDefinitionKey\":\"invoice\"}"))
				.andExpect(status().isOk());

		verify(historyProcessService).findProcessesInstancesHistory(
				eq(Map.of("processDefinitionKey", "invoice")), eq(Optional.empty()), eq(Optional.empty()), any());
	}

	@Test
	public void pagingComesFromTheQueryStringNotTheBody() throws Exception {
		when(historyProcessService.findProcessesInstancesHistory(any(), any(), any(), any())).thenReturn(List.of());

		mvc.perform(post(BASE + "/history/process-instance").param("firstResult", "5").param("maxResults", "10")
						.contentType("application/json").content("{\"finished\":true}"))
				.andExpect(status().isOk());

		verify(historyProcessService).findProcessesInstancesHistory(eq(Map.of("finished", true)),
				eq(Optional.of(5)), eq(Optional.of(10)), any());
	}

	@Test
	public void anAbsentBodyIsTreatedAsNoFilters() throws Exception {
		when(historyProcessService.findProcessesInstancesHistory(any(), any(), any(), any())).thenReturn(List.of());

		mvc.perform(post(BASE + "/history/process-instance").contentType("application/json"))
				.andExpect(status().isOk());

		verify(historyProcessService).findProcessesInstancesHistory(eq(Map.of()), eq(Optional.empty()),
				eq(Optional.empty()), any());
	}

	/** A form written against engine-rest reads the count as an object, not as a bare number. */
	@Test
	public void theCountAnswersInTheEngineShape() throws Exception {
		when(historyProcessService.countProcessesInstancesHistory(any(), any())).thenReturn(7L);

		mvc.perform(post(BASE + "/history/process-instance/count").contentType("application/json")
						.content("{\"processDefinitionKey\":\"invoice\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(7));
	}

	/** The sdk asks for activity instances with parameters, not with a body. */
	@Test
	public void theActivityInstanceQueryIsReadFromTheParameters() throws Exception {
		when(historyProcessService.findActivitiesInstancesHistory(any(), any()))
				.thenReturn(List.of(mock(ActivityInstanceHistory.class)));

		mvc.perform(get(BASE + "/history/activity-instance").param("processInstanceId", "pi-1"))
				.andExpect(status().isOk());

		verify(historyProcessService).findActivitiesInstancesHistory(eq(Map.of("processInstanceId", "pi-1")), any());
	}
}
