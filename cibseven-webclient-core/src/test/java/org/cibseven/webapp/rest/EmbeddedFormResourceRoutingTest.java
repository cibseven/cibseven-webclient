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
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
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
 * Routing and payload-shape coverage for the engine-rest compatible paths embedded forms request.
 *
 * <p>The other tests for these endpoints invoke the controller methods directly, which cannot catch
 * a wrong HTTP verb or a wrong path: both endpoints below were initially written with the wrong verb
 * and still passed those tests. Going through the dispatcher pins the method and path bpm-sdk
 * actually uses, and asserting the serialized JSON pins the field names the sdk reads, so a later
 * DTO change cannot silently leave a form's dropdown empty on a 200.</p>
 */
@ExtendWith(MockitoExtension.class)
public class EmbeddedFormResourceRoutingTest {

	private static final String BASE = "/services/v1";

	@Mock
	private AdminService adminService;

	private MockMvc identityMvc;

	@BeforeEach
	public void setUp() {
		CIBUser user = mock(CIBUser.class);

		// CIBUser is normally supplied by the resolver registered in SevenWebclientContext.
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

		identityMvc = MockMvcBuilders.standaloneSetup(new EmbeddedFormResourceService(adminService))
				.setCustomArgumentResolvers(userResolver)
				.addPlaceholderValue("cibseven.webclient.services.basePath", BASE)
				.build();
	}

	private void stubGroups(UserGroup... groups) {
		when(adminService.findGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any())).thenReturn(List.of(groups));
	}

	private void stubUsers(User... users) {
		when(adminService.findUsers(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(users));
	}

	@Test
	public void groupListIsServedAtTheBarePathWithTheEngineFieldNames() throws Exception {
		stubGroups(new UserGroup("sales", "Sales", "WORKFLOW"));

		identityMvc.perform(get(BASE + "/group").param("member", "demo"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("sales"))
				.andExpect(jsonPath("$[0].name").value("Sales"))
				.andExpect(jsonPath("$[0].type").value("WORKFLOW"));
	}

	@Test
	public void groupListAlsoAcceptsThePostQueryVariantTheSdkUses() throws Exception {
		stubGroups(new UserGroup("sales", "Sales", "WORKFLOW"));

		identityMvc.perform(post(BASE + "/group")
						.contentType("application/json")
						.content("{\"member\":\"demo\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("sales"));

		verify(adminService).findGroups(any(), any(), any(), any(), eq(Optional.of("demo")), any(),
				any(), any(), any(), any(), any(), any());
	}

	@Test
	public void groupCountIsReachableWithGetTheVerbTheSdkUses() throws Exception {
		stubGroups(new UserGroup("a", "A", "WORKFLOW"), new UserGroup("b", "B", "WORKFLOW"));

		identityMvc.perform(get(BASE + "/group/count").param("type", "WORKFLOW"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(2));
	}

	@Test
	public void groupCountIsNotSwallowedByTheGroupIdTemplate() throws Exception {
		stubGroups(new UserGroup("a", "A", "WORKFLOW"));

		// /group/count must resolve to the count mapping, returning a count object rather than a group
		identityMvc.perform(get(BASE + "/group/count"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").exists())
				.andExpect(jsonPath("$.id").doesNotExist());
	}

	@Test
	public void singleGroupLookupReturnsTheGroupAnd404WhenAbsent() throws Exception {
		stubGroups(new UserGroup("sales", "Sales", "WORKFLOW"));
		identityMvc.perform(get(BASE + "/group/sales"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("sales"));

		stubGroups();
		identityMvc.perform(get(BASE + "/group/nope")).andExpect(status().isNotFound());
	}

	@Test
	public void userListIsServedAtTheBarePathWithTheEngineFieldNames() throws Exception {
		stubUsers(new User("demo", "John", "Doe", "john@example.com"));

		identityMvc.perform(get(BASE + "/user").param("memberOfGroup", "sales"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("demo"))
				.andExpect(jsonPath("$[0].firstName").value("John"))
				.andExpect(jsonPath("$[0].lastName").value("Doe"))
				.andExpect(jsonPath("$[0].email").value("john@example.com"));
	}

	@Test
	public void userCountIsReachableWithGet() throws Exception {
		stubUsers(new User("demo", "John", "Doe", "john@example.com"));

		identityMvc.perform(get(BASE + "/user/count"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(1));
	}

	@Test
	public void userProfileIsServedAtTheEnginePathAnd404sWhenAbsent() throws Exception {
		stubUsers(new User("demo", "John", "Doe", "john@example.com"));
		identityMvc.perform(get(BASE + "/user/demo/profile"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("John"));

		stubUsers();
		identityMvc.perform(get(BASE + "/user/nope/profile")).andExpect(status().isNotFound());
	}

	@Test
	public void unmappedIdentityPathsStill404RatherThanBeingProxied() throws Exception {
		// the alias controller is deliberately narrow - it is not a catch-all forwarder
		identityMvc.perform(get(BASE + "/tenant")).andExpect(status().isNotFound());
	}
}
