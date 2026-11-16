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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.BpmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Routing of the start form variables (CIB7-2356). The bpm-sdk behind the embedded and generated
 * start forms sends {@code {id}/form-variables?variableNames=...} and needs the id reading (#1402);
 * a key with names goes through {@code /key/{key}/form-variables}.
 */
public class ProcessDefinitionServiceFormVariablesTest {

	private static final String BASE = "/services/v1/process-definition";

	private BpmProvider bpmProvider;
	private CIBUser user;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		bpmProvider = mock(BpmProvider.class);
		user = new CIBUser("demo");
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

		ProcessDefinitionService service = new ProcessDefinitionService();
		ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);

		mvc = MockMvcBuilders.standaloneSetup(service)
				.setCustomArgumentResolvers(userResolver)
				.addPlaceholderValue("cibseven.webclient.services.basePath", "/services/v1")
				.build();
		when(bpmProvider.fetchProcessFormVariables(anyString(), anyBoolean(), any())).thenReturn(Map.of());
		when(bpmProvider.fetchProcessFormVariables(anyList(), anyString(), anyBoolean(), any())).thenReturn(Map.of());
		when(bpmProvider.fetchProcessFormVariablesByKey(anyList(), anyString(), anyBoolean(), any())).thenReturn(Map.of());
	}

	// ---------- /{x}/form-variables: unchanged, the start forms depend on it ----------

	@Test
	void withoutNamesThePathSegmentIsAKey() throws Exception {
		mvc.perform(get(BASE + "/invoice/form-variables")).andExpect(status().isOk());

		verify(bpmProvider).fetchProcessFormVariables(eq("invoice"), eq(true), eq(user));
		verifyNoMoreInteractions(bpmProvider);
	}

	@Test
	void withNamesThePathSegmentIsAnIdAsTheStartFormsSendIt() throws Exception {
		// the exact query the bpm-sdk CamundaForm.fetchVariables sends for a start form
		mvc.perform(get(BASE + "/invoice:1:abc/form-variables")
				.param("deserializeValues", "false")
				.param("variableNames", "input1,input2"))
			.andExpect(status().isOk());

		verify(bpmProvider).fetchProcessFormVariables(eq(List.of("input1", "input2")), eq("invoice:1:abc"), eq(false), eq(user));
		verify(bpmProvider, never()).fetchProcessFormVariablesByKey(anyList(), anyString(), anyBoolean(), any());
		verifyNoMoreInteractions(bpmProvider);
	}

	@Test
	void withASingleNameThePathSegmentIsStillAnId() throws Exception {
		mvc.perform(get(BASE + "/invoice:1:abc/form-variables").param("variableNames", "input1"))
			.andExpect(status().isOk());

		verify(bpmProvider).fetchProcessFormVariables(eq(List.of("input1")), eq("invoice:1:abc"), eq(true), eq(user));
		verifyNoMoreInteractions(bpmProvider);
	}

	@Test
	void anEmptyNameListCountsAsNoNames() throws Exception {
		mvc.perform(get(BASE + "/invoice/form-variables").param("variableNames", ""))
			.andExpect(status().isOk());

		verify(bpmProvider).fetchProcessFormVariables(eq("invoice"), eq(true), eq(user));
		verifyNoMoreInteractions(bpmProvider);
	}

	// ---------- /key/{key}/form-variables: always a key (CIB7-2356) ----------

	@Test
	void theKeyRouteWithNamesResolvesAKey() throws Exception {
		mvc.perform(get(BASE + "/key/invoice/form-variables")
				.param("variableNames", "input1"))
			.andExpect(status().isOk());

		verify(bpmProvider).fetchProcessFormVariablesByKey(eq(List.of("input1")), eq("invoice"), eq(true), eq(user));
		verify(bpmProvider, never()).fetchProcessFormVariables(anyList(), anyString(), anyBoolean(), any());
		verifyNoMoreInteractions(bpmProvider);
	}

	@Test
	void theKeyRouteForwardsSeveralNamesAndTheDeserializeFlag() throws Exception {
		// the query the bpm-sdk sends when it has a key instead of an id
		mvc.perform(get(BASE + "/key/invoice/form-variables")
				.param("deserializeValues", "false")
				.param("variableNames", "input1,input2"))
			.andExpect(status().isOk());

		verify(bpmProvider).fetchProcessFormVariablesByKey(eq(List.of("input1", "input2")), eq("invoice"), eq(false), eq(user));
		verifyNoMoreInteractions(bpmProvider);
	}

	@Test
	void theKeyRouteWithoutNamesResolvesAKey() throws Exception {
		mvc.perform(get(BASE + "/key/invoice/form-variables")).andExpect(status().isOk());

		verify(bpmProvider).fetchProcessFormVariables(eq("invoice"), eq(true), eq(user));
		verifyNoMoreInteractions(bpmProvider);
	}

	@Test
	void theKeyRouteWithAnEmptyNameListResolvesAKeyWithoutNames() throws Exception {
		mvc.perform(get(BASE + "/key/invoice/form-variables")
				.param("deserializeValues", "false")
				.param("variableNames", ""))
			.andExpect(status().isOk());

		verify(bpmProvider).fetchProcessFormVariables(eq("invoice"), eq(false), eq(user));
		verifyNoMoreInteractions(bpmProvider);
	}

	@Test
	void aKeyNamedKeyIsNotMistakenForTheKeyRoute() throws Exception {
		// "/key/form-variables" has one segment before form-variables, so it is the key "key"
		mvc.perform(get(BASE + "/key/form-variables")).andExpect(status().isOk());

		verify(bpmProvider).fetchProcessFormVariables(eq("key"), eq(true), eq(user));
		verifyNoMoreInteractions(bpmProvider);
	}
}
