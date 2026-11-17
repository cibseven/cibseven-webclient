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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * GET system/pool-status through the dispatcher, with the user taken from the Authorization header
 * as SevenWebclientContext does.
 */
@ExtendWith(MockitoExtension.class)
class SystemServiceTest {

	private static final String POOL_STATUS = "/services/v1/system/pool-status";
	private static final String STATS = "MaxTotal: 40, DefaultMaxPerRoute: 20, Available: 3, Leased: 0, Pending: 0";

	@Mock
	private CustomRestTemplate restTemplate;

	private MockMvc mvc;

	/** The status ExceptionConverter (web module) answers an AuthenticationException with. */
	@RestControllerAdvice
	static class StatusAdvice {
		@ExceptionHandler @ResponseStatus(HttpStatus.UNAUTHORIZED)
		void unauthorized(AuthenticationException x) {
		}
	}

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
				if (request.getHeader("Authorization") == null) {
					throw new AuthenticationException();
				}
				return new CIBUser("demo");
			}
		};

		mvc = MockMvcBuilders.standaloneSetup(new SystemService(restTemplate))
				.setCustomArgumentResolvers(userResolver)
				.setControllerAdvice(new StatusAdvice())
				.build();
	}

	@Test
	void poolStatus_withoutLogin_answers401() throws Exception {
		mvc.perform(get(POOL_STATUS))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(restTemplate);
	}

	@Test
	void poolStatus_loggedIn_returnsThePoolStatisticsWithoutAnyOtherCallThroughThePool() throws Exception {
		when(restTemplate.getConnectionPoolStats()).thenReturn(STATS);

		mvc.perform(get(POOL_STATUS).header("Authorization", "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(content().string(STATS));

		verify(restTemplate).getConnectionPoolStats();
		verifyNoMoreInteractions(restTemplate);
	}
}
