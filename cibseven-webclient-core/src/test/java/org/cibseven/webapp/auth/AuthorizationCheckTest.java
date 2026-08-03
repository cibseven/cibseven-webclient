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
package org.cibseven.webapp.auth;

import java.util.List;

import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The resource-aware permission evaluation the webclient needs for resources the engine cannot
 * decide about. Covers what the deprecated {@code SevenAuthorizationUtils#checkPermission} got
 * wrong: it matched on resource type and permission only, so a permission on one resource id
 * satisfied a check for another.
 */
class AuthorizationCheckTest {

	private static final int TYPE_GLOBAL = 0;
	private static final int TYPE_GRANT = 1;
	private static final int TYPE_REVOKE = 2;

	@Test
	void grantOnTheRequestedResourceAuthorizes() {
		assertTrue(isAuthorized(applications(auth(TYPE_GRANT, "modeler", "ACCESS")), "modeler", "ACCESS"));
	}

	/** The gap this evaluation closes: another resource id must not satisfy the check. */
	@Test
	void grantOnAnotherResourceDoesNotAuthorize() {
		assertFalse(isAuthorized(applications(auth(TYPE_GRANT, "tasklist", "ACCESS")), "modeler", "ACCESS"));
	}

	@Test
	void wildcardResourceAuthorizes() {
		assertTrue(isAuthorized(applications(auth(TYPE_GRANT, "*", "ACCESS")), "modeler", "ACCESS"));
	}

	@Test
	void allPermissionCoversTheRequestedOne() {
		assertTrue(isAuthorized(applications(auth(TYPE_GRANT, "modeler", "ALL")), "modeler", "ACCESS"));
	}

	@Test
	void anotherPermissionDoesNotAuthorize() {
		assertFalse(isAuthorized(applications(auth(TYPE_GRANT, "modeler", "READ")), "modeler", "ACCESS"));
	}

	@Test
	void globalAuthorizationAuthorizes() {
		assertTrue(isAuthorized(applications(auth(TYPE_GLOBAL, "*", "ALL")), "modeler", "ACCESS"));
	}

	@Test
	void revokeWinsOverGrant() {
		assertFalse(isAuthorized(applications(
			auth(TYPE_GRANT, "modeler", "ACCESS"), auth(TYPE_REVOKE, "modeler", "ACCESS")), "modeler", "ACCESS"));
	}

	@Test
	void revokeOnAnotherResourceDoesNotApply() {
		assertTrue(isAuthorized(applications(
			auth(TYPE_GRANT, "modeler", "ACCESS"), auth(TYPE_REVOKE, "cockpit", "ALL")), "modeler", "ACCESS"));
	}

	@Test
	void withoutAuthorizationsNothingIsAuthorized() {
		assertFalse(isAuthorized(applications(), "modeler", "ACCESS"));
		assertFalse(isAuthorized(new Authorizations(), "modeler", "ACCESS"));
		assertFalse(isAuthorized(null, "modeler", "ACCESS"));
	}

	/** Resource types other than the application one resolve to their own authorization list. */
	@Test
	void evaluatesOtherResourceTypes() {
		Authorizations authorizations = new Authorizations();
		authorizations.setProcessDefinition(List.of(auth(TYPE_GRANT, "invoice", "CREATE_INSTANCE")));
		authorizations.setApplication(List.of());

		assertTrue(AuthorizationCheck.isAuthorized(authorizations, SevenResourceType.PROCESS_DEFINITION,
			"invoice", "CREATE_INSTANCE"));
		assertFalse(AuthorizationCheck.isAuthorized(authorizations, SevenResourceType.PROCESS_DEFINITION,
			"payroll", "CREATE_INSTANCE"));
		// The grant belongs to process definitions, not to applications.
		assertFalse(AuthorizationCheck.isAuthorized(authorizations, SevenResourceType.APPLICATION,
			"invoice", "CREATE_INSTANCE"));
	}

	private static boolean isAuthorized(Authorizations authorizations, String resourceId, String permission) {
		return AuthorizationCheck.isAuthorized(authorizations, SevenResourceType.APPLICATION, resourceId, permission);
	}

	private static Authorizations applications(Authorization... authorizations) {
		Authorizations result = new Authorizations();
		result.setApplication(List.of(authorizations));
		return result;
	}

	private static Authorization auth(int type, String resourceId, String... permissions) {
		Authorization authorization = new Authorization();
		authorization.setType(type);
		authorization.setResourceId(resourceId);
		authorization.setPermissions(permissions);
		return authorization;
	}
}
