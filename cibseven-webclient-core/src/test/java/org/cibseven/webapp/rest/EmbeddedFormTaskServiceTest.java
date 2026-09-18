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
 * The engine-rest shaped task paths an embedded form reaches through bpm-sdk. The sdk sends the
 * user id in the body of claim and delegate and reads the local variables and comments with a
 * plain GET, so those shapes are pinned through the real dispatcher.
 */
@ExtendWith(MockitoExtension.class)
public class EmbeddedFormTaskServiceTest {

	private static final String BASE = "/services/v1";
	private static final String TASK = BASE + "/task/task-1";

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

		EmbeddedFormTaskService service = new EmbeddedFormTaskService();
		ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);

		mvc = MockMvcBuilders.standaloneSetup(service)
				.setCustomArgumentResolvers(userResolver)
				.addPlaceholderValue("cibseven.webclient.services.basePath", BASE)
				.build();
	}

	@Test
	public void claimTakesTheUserFromTheBody() throws Exception {
		mvc.perform(post(TASK + "/claim").contentType("application/json").content("{\"userId\":\"demo\"}"))
				.andExpect(status().isOk());

		verify(bpmProvider).claim(eq("task-1"), eq("demo"), any());
	}

	/** Unclaiming is the assignee reset, so it goes through the call the curated endpoint uses. */
	@Test
	public void unclaimResetsTheAssignee() throws Exception {
		mvc.perform(post(TASK + "/unclaim")).andExpect(status().isOk());

		verify(bpmProvider).setAssignee(eq("task-1"), eq("null"), any());
	}

	@Test
	public void delegateTakesTheUserFromTheBody() throws Exception {
		mvc.perform(post(TASK + "/delegate").contentType("application/json").content("{\"userId\":\"other\"}"))
				.andExpect(status().isOk());

		verify(bpmProvider).delegate(eq("task-1"), eq("other"), any());
	}

	@Test
	public void localVariablesAreReadByName() throws Exception {
		when(bpmProvider.findLocalVariables(eq("task-1"), any()))
				.thenReturn(Map.of("amount", Map.of("value", 42, "type", "Integer")));

		mvc.perform(get(TASK + "/localVariables"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.amount.value").value(42));
	}

	@Test
	public void commentsAreHandedBackAsTheEngineReturnsThem() throws Exception {
		when(bpmProvider.findComments(eq("task-1"), any()))
				.thenReturn(List.of(Map.of("id", "c1", "message", "looks good")));

		mvc.perform(get(TASK + "/comment"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].message").value("looks good"));
	}
}
