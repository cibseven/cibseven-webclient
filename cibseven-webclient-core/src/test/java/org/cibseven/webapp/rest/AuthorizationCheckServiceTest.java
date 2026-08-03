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

import java.util.List;
import java.util.Optional;

import org.cibseven.webapp.auth.AuthorizationChecker;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.UnknownResourceTypeException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.AuthorizationCheckResult;
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The middleware's own answer to "may I do this?", for the authenticated caller. The engine offers
 * the same operation but only for callers it authenticated itself, which a middleware talking to an
 * engine-rest without an authentication filter cannot rely on.
 */
class AuthorizationCheckServiceTest {

	private static final CIBUser USER = new CIBUser("demo");
	private static final int APPLICATION = 0;
	private static final int PROCESS_DEFINITION = 6;

	private AuthorizationCheckService service;
	private BpmProvider bpmProvider;

	@BeforeEach
	void setUp() {
		service = new AuthorizationCheckService();
		bpmProvider = mock(BpmProvider.class);
		ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(service, "authorizationChecker",
			new AuthorizationChecker(bpmProvider, true, false));
		engineAuthorization(true);
	}

	@Test
	void reportsTrueForAGrantedPermission() {
		userAuthorizations(auth(1, "modeler", "ACCESS"));

		AuthorizationCheckResult result = check("ACCESS", APPLICATION, "modeler");

		assertTrue(result.isAuthorized());
		assertEquals("ACCESS", result.getPermissionName());
		assertEquals("modeler", result.getResourceId());
		assertEquals("application", result.getResourceName());
	}

	@Test
	void reportsFalseForAnotherResource() {
		userAuthorizations(auth(1, "tasklist", "ACCESS"));

		assertFalse(check("ACCESS", APPLICATION, "modeler").isAuthorized());
	}

	@Test
	void reportsFalseWhenRevoked() {
		userAuthorizations(auth(1, "modeler", "ACCESS"), auth(2, "*", "ALL"));

		assertFalse(check("ACCESS", APPLICATION, "modeler").isAuthorized());
	}

	/** Nothing is enforced without engine authorization, so the answer must not claim otherwise. */
	@Test
	void reportsTrueWhenEngineAuthorizationIsDisabled() {
		engineAuthorization(false);

		assertTrue(check("ACCESS", APPLICATION, "modeler").isAuthorized());
	}

	@Test
	void resolvesResourceTypesOtherThanApplication() {
		Authorizations authorizations = new Authorizations();
		authorizations.setProcessDefinition(List.of(auth(1, "invoice", "CREATE_INSTANCE")));
		when(bpmProvider.getUserAuthorization(USER.getId(), USER)).thenReturn(authorizations);

		AuthorizationCheckResult result = check("CREATE_INSTANCE", PROCESS_DEFINITION, "invoice");

		assertTrue(result.isAuthorized());
		assertEquals("process_definition", result.getResourceName());
	}

	@Test
	void anExplicitResourceNameIsEchoedBack() {
		userAuthorizations(auth(1, "modeler", "ACCESS"));

		AuthorizationCheckResult result = service.isUserAuthorized("ACCESS", APPLICATION,
			Optional.of("modeler"), Optional.of("application"), USER);

		assertEquals("application", result.getResourceName());
	}

	@Test
	void unknownResourceTypeIsRejected() {
		assertThrows(UnknownResourceTypeException.class, () -> check("ACCESS", 99, "modeler"));
	}

	private AuthorizationCheckResult check(String permission, int resourceType, String resourceId) {
		return service.isUserAuthorized(permission, resourceType, Optional.ofNullable(resourceId),
			Optional.empty(), USER);
	}

	private void engineAuthorization(boolean enabled) {
		EngineConfiguration configuration = new EngineConfiguration();
		configuration.setAuthorizationEnabled(enabled);
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(configuration);
	}

	private void userAuthorizations(Authorization... authorizations) {
		Authorizations result = new Authorizations();
		result.setApplication(List.of(authorizations));
		when(bpmProvider.getUserAuthorization(USER.getId(), USER)).thenReturn(result);
	}

	private static Authorization auth(int type, String resourceId, String... permissions) {
		Authorization authorization = new Authorization();
		authorization.setType(type);
		authorization.setResourceId(resourceId);
		authorization.setPermissions(permissions);
		return authorization;
	}
}
