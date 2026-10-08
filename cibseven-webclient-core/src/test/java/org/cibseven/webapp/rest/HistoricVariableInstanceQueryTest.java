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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.VariableHistory;
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
 * The historic variable query an embedded form reaches through bpm-sdk. The sdk posts the filters
 * as the body and puts paging and the deserialization flag in the query string, so the verb and
 * that split are pinned through the real dispatcher: the path used to serve only a read by id,
 * which is why the list call came back as 404 (CIB7-1994).
 */
@ExtendWith(MockitoExtension.class)
public class HistoricVariableInstanceQueryTest {

	private static final String BASE = "/services/v1";
	private static final String PATH = BASE + "/history/variable-instance";

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

		HistoricVariableInstanceService service = new HistoricVariableInstanceService();
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
		when(bpmProvider.findHistoricVariableInstances(any(), any(), any(), any(), any()))
				.thenReturn(List.of(mock(VariableHistory.class)));

		mvc.perform(post(PATH).contentType("application/json")
						.content("{\"processInstanceId\":\"pi-1\"}"))
				.andExpect(status().isOk());
	}

	@Test
	public void everyBodyFilterIsForwardedToTheProvider() throws Exception {
		when(bpmProvider.findHistoricVariableInstances(any(), any(), any(), any(), any())).thenReturn(List.of());

		mvc.perform(post(PATH).contentType("application/json")
						.content("{\"processInstanceId\":\"pi-1\",\"variableName\":\"amount\"}"))
				.andExpect(status().isOk());

		verify(bpmProvider).findHistoricVariableInstances(
				eq(Map.of("processInstanceId", "pi-1", "variableName", "amount")),
				eq(Optional.empty()), eq(Optional.empty()), isNull(), any());
	}

	/** The sdk sends these three in the query string and everything else in the body. */
	@Test
	public void pagingAndDeserializationComeFromTheQueryString() throws Exception {
		when(bpmProvider.findHistoricVariableInstances(any(), any(), any(), any(), any())).thenReturn(List.of());

		mvc.perform(post(PATH).param("firstResult", "10").param("maxResults", "25")
						.param("deserializeValues", "false")
						.contentType("application/json").content("{\"processInstanceId\":\"pi-1\"}"))
				.andExpect(status().isOk());

		verify(bpmProvider).findHistoricVariableInstances(eq(Map.of("processInstanceId", "pi-1")),
				eq(Optional.of(10)), eq(Optional.of(25)), eq(Boolean.FALSE), any());
	}

	@Test
	public void anAbsentBodyIsTreatedAsNoFilters() throws Exception {
		when(bpmProvider.findHistoricVariableInstances(any(), any(), any(), any(), any())).thenReturn(List.of());

		mvc.perform(post(PATH).contentType("application/json")).andExpect(status().isOk());

		verify(bpmProvider).findHistoricVariableInstances(eq(Map.of()), eq(Optional.empty()),
				eq(Optional.empty()), isNull(), any());
	}

	/** A form written against engine-rest reads the count as an object, not as a bare number. */
	@Test
	public void countIsServedWithPostAndTheEngineCountShape() throws Exception {
		when(bpmProvider.findHistoricVariableInstancesCount(any(), any())).thenReturn(4);

		mvc.perform(post(PATH + "/count").contentType("application/json")
						.content("{\"processInstanceId\":\"pi-1\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(4));

		verify(bpmProvider).findHistoricVariableInstancesCount(eq(Map.of("processInstanceId", "pi-1")), any());
	}

	/** Reading one instance by id keeps working next to the query on the same path. */
	@Test
	public void theReadByIdStillAnswers() throws Exception {
		when(bpmProvider.getHistoricVariableInstance(eq("var-1"), eq(true), any()))
				.thenReturn(mock(VariableHistory.class));

		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(PATH + "/var-1"))
				.andExpect(status().isOk());
	}
}
