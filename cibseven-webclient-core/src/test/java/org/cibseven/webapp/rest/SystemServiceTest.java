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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cibseven.webapp.auth.AuthorizationChecker;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.providers.BpmProvider;
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
 * GET system/pool-status through the dispatcher: the user comes from the Authorization header as in
 * SevenWebclientContext, and the permission decision is the real AuthorizationChecker asking the
 * (mocked) engine.
 */
@ExtendWith(MockitoExtension.class)
class SystemServiceTest {

	private static final String POOL_STATUS = "/services/v1/system/pool-status";
	private static final String STATS = "MaxTotal: 40, DefaultMaxPerRoute: 20, Available: 3, Leased: 0, Pending: 0";

	@Mock
	private BpmProvider bpmProvider;

	@Mock
	private CustomRestTemplate restTemplate;

	private final CIBUser user = new CIBUser("demo");
	private MockMvc mvc;

	/** Status codes as ExceptionConverter (web module) maps them. */
	@RestControllerAdvice
	static class StatusAdvice {
		@ExceptionHandler @ResponseStatus(HttpStatus.UNAUTHORIZED)
		void unauthorized(AuthenticationException x) {
		}

		@ExceptionHandler @ResponseStatus(HttpStatus.FORBIDDEN)
		void forbidden(AccessDeniedException x) {
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
				return user;
			}
		};

		SystemService service = new SystemService(restTemplate, new AuthorizationChecker(bpmProvider));
		mvc = MockMvcBuilders.standaloneSetup(service)
				.setCustomArgumentResolvers(userResolver)
				.setControllerAdvice(new StatusAdvice())
				.build();
	}

	@Test
	void poolStatus_withoutLogin_answers401() throws Exception {
		mvc.perform(get(POOL_STATUS))
				.andExpect(status().isUnauthorized());

		verify(bpmProvider, never()).isUserAuthorized(any(), anyInt(), anyString(), anyString());
		verify(restTemplate, never()).getConnectionPoolStats();
	}

	@Test
	void poolStatus_withoutSystemRead_answers403() throws Exception {
		when(bpmProvider.isUserAuthorized(user, SevenResourceType.SYSTEM.getType(), "*", "READ")).thenReturn(false);

		mvc.perform(get(POOL_STATUS).header("Authorization", "Bearer token"))
				.andExpect(status().isForbidden());

		verify(restTemplate, never()).getConnectionPoolStats();
	}

	@Test
	void poolStatus_withSystemRead_returnsThePoolStatistics() throws Exception {
		when(bpmProvider.isUserAuthorized(user, SevenResourceType.SYSTEM.getType(), "*", "READ")).thenReturn(true);
		when(restTemplate.getConnectionPoolStats()).thenReturn(STATS);

		mvc.perform(get(POOL_STATUS).header("Authorization", "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(content().string(STATS));
	}
}
