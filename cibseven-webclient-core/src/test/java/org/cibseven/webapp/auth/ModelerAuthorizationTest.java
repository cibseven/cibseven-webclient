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
 * The permission matrix behind the modeler REST access check. It has to match what the frontend
 * evaluates in permissions.js, otherwise a user is shown a modeler that answers 403.
 */
class ModelerAuthorizationTest {

	private static final int TYPE_GLOBAL = 0;
	private static final int TYPE_GRANT = 1;
	private static final int TYPE_REVOKE = 2;

	@Test
	void grantsAccessForModelerResource() {
		assertTrue(ModelerAuthorization.hasModelerAccess(applications(auth(TYPE_GRANT, "modeler", "ACCESS"))));
	}

	@Test
	void grantsAccessForAllApplicationsWildcard() {
		assertTrue(ModelerAuthorization.hasModelerAccess(applications(auth(TYPE_GRANT, "*", "ACCESS"))));
	}

	@Test
	void grantsAccessForAllPermission() {
		assertTrue(ModelerAuthorization.hasModelerAccess(applications(auth(TYPE_GRANT, "modeler", "ALL"))));
	}

	/**
	 * The payload a camunda-admin member actually gets from the engine: a group grant on every
	 * application resource. Taken from a running CIB seven 2.3.0 instance.
	 */
	@Test
	void grantsAccessForAdminGroupAuthorization() {
		assertTrue(ModelerAuthorization.hasModelerAccess(applications(auth(TYPE_GRANT, "*", "ALL"))));
	}

	@Test
	void grantsAccessForGlobalAuthorization() {
		assertTrue(ModelerAuthorization.hasModelerAccess(applications(auth(TYPE_GLOBAL, "*", "ALL"))));
	}

	@Test
	void grantsAccessWhenOneOfSeveralAuthorizationsMatches() {
		assertTrue(ModelerAuthorization.hasModelerAccess(applications(
			auth(TYPE_GRANT, "tasklist", "ACCESS"),
			auth(TYPE_GRANT, "modeler", "ACCESS"))));
	}

	/**
	 * The gap this check closes: access to another application is not access to the modeler.
	 */
	@Test
	void deniesAccessForOtherApplicationsOnly() {
		assertFalse(ModelerAuthorization.hasModelerAccess(applications(
			auth(TYPE_GRANT, "tasklist", "ACCESS"),
			auth(TYPE_GRANT, "cockpit", "ALL"))));
	}

	@Test
	void deniesAccessWithoutApplicationAuthorizations() {
		assertFalse(ModelerAuthorization.hasModelerAccess(applications()));
		assertFalse(ModelerAuthorization.hasModelerAccess(new Authorizations()));
		assertFalse(ModelerAuthorization.hasModelerAccess(null));
	}

	@Test
	void deniesAccessForUnrelatedPermission() {
		assertFalse(ModelerAuthorization.hasModelerAccess(applications(auth(TYPE_GRANT, "modeler", "READ"))));
	}

	@Test
	void deniesAccessForEmptyPermissions() {
		assertFalse(ModelerAuthorization.hasModelerAccess(applications(auth(TYPE_GRANT, "modeler"))));
	}

	@Test
	void revokeWinsOverGrant() {
		assertFalse(ModelerAuthorization.hasModelerAccess(applications(
			auth(TYPE_GRANT, "modeler", "ACCESS"),
			auth(TYPE_REVOKE, "modeler", "ACCESS"))));
	}

	@Test
	void revokeOnWildcardWinsOverModelerGrant() {
		assertFalse(ModelerAuthorization.hasModelerAccess(applications(
			auth(TYPE_GRANT, "modeler", "ACCESS"),
			auth(TYPE_REVOKE, "*", "ALL"))));
	}

	@Test
	void revokeOfAnotherApplicationDoesNotAffectModeler() {
		assertTrue(ModelerAuthorization.hasModelerAccess(applications(
			auth(TYPE_GRANT, "modeler", "ACCESS"),
			auth(TYPE_REVOKE, "cockpit", "ACCESS"))));
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
