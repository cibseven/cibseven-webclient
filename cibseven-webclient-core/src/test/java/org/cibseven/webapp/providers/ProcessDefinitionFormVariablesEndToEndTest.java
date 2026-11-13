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
package org.cibseven.webapp.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.ProcessDefinitionService;
import org.junit.jupiter.api.AfterEach;
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
 * Start form variables from the services route down to the engine-rest call (CIB7-2356, #1402).
 * Runs the real controller, the real {@link BpmProvider} delegation and the real
 * {@link VariableProvider} against a mock engine, and checks which engine path each services
 * path ends up on. #1362 sent the id path the start forms use to the engine's key endpoint;
 * the mocked-provider tests could not see that, this one does.
 */
public class ProcessDefinitionFormVariablesEndToEndTest {

	private static final String BASE = "/services/v1/process-definition";
	private static final String DEFINITION_ID = "e2e-start-two-field-form:1:5f0a";
	private static final String TWO_FIELDS =
			"{\"input1\":{\"type\":\"String\",\"value\":\"Default value for input1\"},"
			+ "\"input2\":{\"type\":\"String\",\"value\":\"Default value for input2\"}}";

	private MockEngineRest engine;
	private MockMvc mvc;

	@BeforeEach
	void setUp() throws Exception {
		engine = new MockEngineRest();
		VariableProvider variableProvider = engine.wire(new VariableProvider());
		CIBUser user = MockEngineRest.user();

		// only getVariableProvider is stubbed; the form variable methods run their default delegation
		BpmProvider bpmProvider = mock(BpmProvider.class, CALLS_REAL_METHODS);
		when(bpmProvider.getVariableProvider()).thenReturn(variableProvider);

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
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.close();
	}

	@Test
	void theStartFormRequestByIdReachesTheEnginesIdEndpoint() throws Exception {
		engine.enqueueJson(TWO_FIELDS);

		// what the bpm-sdk sends for an embedded or generated start form (#1402)
		mvc.perform(get(BASE + "/" + DEFINITION_ID + "/form-variables")
				.param("deserializeValues", "false")
				.param("variableNames", "input1,input2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.input1.value").value("Default value for input1"))
			.andExpect(jsonPath("$.input2.value").value("Default value for input2"));

		String path = engine.takePath();
		assertThat(path).startsWith("/process-definition/" + DEFINITION_ID + "/form-variables?");
		assertThat(path).doesNotContain("/key/");
		assertThat(path).contains("deserializeValues=false").contains("variableNames=input1,input2");
		assertThat(engine.requestCount()).isEqualTo(1);
	}

	@Test
	void aKeyWithNamesReachesTheEnginesKeyEndpoint() throws Exception {
		engine.enqueueJson("{\"input1\":{\"type\":\"String\",\"value\":\"Default value for input1\"}}");

		// CIB7-2356: a key with names, through the new route
		mvc.perform(get(BASE + "/key/e2e-start-two-field-form/form-variables")
				.param("variableNames", "input1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.input1.value").value("Default value for input1"))
			.andExpect(jsonPath("$.input2").doesNotExist());

		assertThat(engine.takePath())
			.isEqualTo("/process-definition/key/e2e-start-two-field-form/form-variables?deserializeValues=true&variableNames=input1");
		assertThat(engine.requestCount()).isEqualTo(1);
	}

	@Test
	void aKeyWithSeveralNamesKeepsThemInOrderWithoutATrailingComma() throws Exception {
		engine.enqueueJson(TWO_FIELDS);

		mvc.perform(get(BASE + "/key/e2e-start-two-field-form/form-variables")
				.param("deserializeValues", "false")
				.param("variableNames", "input2,input1"))
			.andExpect(status().isOk());

		assertThat(engine.takePath())
			.isEqualTo("/process-definition/key/e2e-start-two-field-form/form-variables?deserializeValues=false&variableNames=input2,input1");
	}

	@Test
	void aKeyWithoutNamesReachesTheEnginesKeyEndpointOnBothRoutes() throws Exception {
		engine.enqueueJson(TWO_FIELDS);
		engine.enqueueJson(TWO_FIELDS);

		mvc.perform(get(BASE + "/e2e-start-two-field-form/form-variables"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.input1").exists())
			.andExpect(jsonPath("$.input2").exists());
		mvc.perform(get(BASE + "/key/e2e-start-two-field-form/form-variables"))
			.andExpect(status().isOk());

		assertThat(engine.takePath()).isEqualTo("/process-definition/key/e2e-start-two-field-form/form-variables?deserializeValues=true");
		assertThat(engine.takePath()).isEqualTo("/process-definition/key/e2e-start-two-field-form/form-variables?deserializeValues=true");
	}

	@Test
	void theCallCarriesTheUsersEngineToken() throws Exception {
		engine.enqueueJson(TWO_FIELDS);

		mvc.perform(get(BASE + "/key/e2e-start-two-field-form/form-variables").param("variableNames", "input1"))
			.andExpect(status().isOk());

		assertThat(engine.take().getHeader("Authorization")).isEqualTo(MockEngineRest.AUTH_TOKEN);
	}
}
