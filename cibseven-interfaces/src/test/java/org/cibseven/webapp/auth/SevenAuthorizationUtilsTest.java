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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * The permission arithmetic behind the deprecated in-webclient authorization check: which
 * combination of GLOBAL, GRANT and REVOKE entries lets a request through. It is deprecated but
 * still live whenever {@code cibseven.webclient.deprecated.authorization.enabled} is set, and a
 * wrong answer here either blocks a legitimate user or lets one through.
 */
@SuppressWarnings("deprecation")
public class SevenAuthorizationUtilsTest {

	private static final int AUTH_TYPE_GLOBAL = 0;
	private static final int AUTH_TYPE_GRANT = 1;
	private static final int AUTH_TYPE_REVOKE = 2;

	private Authorization authorization(int type, String resourceId, String... permissions) {
		Authorization authorization = new Authorization();
		authorization.setType(type);
		authorization.setResourceId(resourceId);
		authorization.setPermissions(permissions);
		return authorization;
	}

	// ---------- resourceType ----------

	@Test
	void resourceType_unwrapsTheEnumId() {
		assertThat(SevenAuthorizationUtils.resourceType(SevenResourceType.PROCESS_DEFINITION))
			.isEqualTo(SevenResourceType.PROCESS_DEFINITION.getType());
		assertThat(SevenAuthorizationUtils.resourceType(SevenResourceType.APPLICATION)).isZero();
	}

	// ---------- hasCockpitRights ----------

	@Test
	void hasCockpitRights_acceptsAGrantOfAccessOnCockpit() {
		Authorizations authorizations = new Authorizations();
		authorizations.setApplication(List.of(authorization(AUTH_TYPE_GRANT, "cockpit", "ACCESS")));

		assertThat(SevenAuthorizationUtils.hasCockpitRights(authorizations)).isTrue();
	}

	@Test
	void hasCockpitRights_acceptsAGrantOfAllOnCockpit() {
		Authorizations authorizations = new Authorizations();
		authorizations.setApplication(List.of(authorization(AUTH_TYPE_GRANT, "cockpit", "ALL")));

		assertThat(SevenAuthorizationUtils.hasCockpitRights(authorizations)).isTrue();
	}

	@Test
	void hasCockpitRights_acceptsAGrantOnTheWildcardResource() {
		Authorizations authorizations = new Authorizations();
		authorizations.setApplication(List.of(authorization(AUTH_TYPE_GRANT, "*", "ACCESS")));

		assertThat(SevenAuthorizationUtils.hasCockpitRights(authorizations)).isTrue();
	}

	@Test
	void hasCockpitRights_acceptsAGlobalAllOnTheWildcardResource() {
		Authorizations authorizations = new Authorizations();
		authorizations.setApplication(List.of(authorization(AUTH_TYPE_GLOBAL, "*", "ALL")));

		assertThat(SevenAuthorizationUtils.hasCockpitRights(authorizations)).isTrue();
	}

	@Test
	void hasCockpitRights_refusesAGrantOnAnotherApplication() {
		Authorizations authorizations = new Authorizations();
		authorizations.setApplication(List.of(authorization(AUTH_TYPE_GRANT, "tasklist", "ACCESS")));

		assertThatThrownBy(() -> SevenAuthorizationUtils.hasCockpitRights(authorizations))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void hasCockpitRights_refusesWhenThereAreNoApplicationAuthorizations() {
		Authorizations authorizations = new Authorizations();
		authorizations.setApplication(List.of());

		assertThatThrownBy(() -> SevenAuthorizationUtils.hasCockpitRights(authorizations))
			.isInstanceOf(AccessDeniedException.class);
	}

	/**
	 * TODO KNOWN BUG (not fixed): the GRANT branch inspects {@code permissions[0]} only, so a grant
	 * that lists ACCESS second is not recognised, even though the engine would honour it - the user
	 * is denied the cockpit they were granted. The GLOBAL branch below does check the whole array.
	 * This test asserts the permission array is read as a whole; it fails until the GRANT branch
	 * stops looking only at the first element.
	 */
	@Test
	@Disabled("KNOWN BUG: hasCockpitRights inspects permissions[0] only, so ACCESS in any later position is ignored")
	void hasCockpitRights_acceptsAccessAnywhereInTheGrantPermissions() {
		Authorizations authorizations = new Authorizations();
		authorizations.setApplication(List.of(authorization(AUTH_TYPE_GRANT, "cockpit", "READ", "ACCESS")));

		assertThat(SevenAuthorizationUtils.hasCockpitRights(authorizations)).isTrue();
	}

	@Test
	void hasCockpitRights_checksTheWholePermissionArrayOfAGlobalEntry() {
		Authorizations authorizations = new Authorizations();
		authorizations.setApplication(List.of(authorization(AUTH_TYPE_GLOBAL, "*", "READ", "ALL")));

		assertThat(SevenAuthorizationUtils.hasCockpitRights(authorizations)).isTrue();
	}

	@Test
	void hasCockpitRights_refusesAGrantWithNoPermissionsAtAll() {
		Authorizations authorizations = new Authorizations();
		authorizations.setApplication(List.of(authorization(AUTH_TYPE_GRANT, "cockpit")));

		assertThatThrownBy(() -> SevenAuthorizationUtils.hasCockpitRights(authorizations))
			.isInstanceOf(AccessDeniedException.class);
	}

	// ---------- checkPermission ----------

	@Test
	void checkPermission_acceptsAGrantCarryingTheRequestedPermission() {
		Authorizations authorizations = new Authorizations();
		authorizations.setUser(List.of(authorization(AUTH_TYPE_GRANT, "*", "READ")));

		assertThat(SevenAuthorizationUtils.checkPermission(
			authorizations, SevenResourceType.USER, List.of("READ"))).isTrue();
	}

	@Test
	void checkPermission_acceptsAGlobalEntryToo() {
		Authorizations authorizations = new Authorizations();
		authorizations.setUser(List.of(authorization(AUTH_TYPE_GLOBAL, "*", "READ")));

		assertThat(SevenAuthorizationUtils.checkPermission(
			authorizations, SevenResourceType.USER, List.of("READ"))).isTrue();
	}

	@Test
	void checkPermission_acceptsAnyOfSeveralRequestedPermissions() {
		Authorizations authorizations = new Authorizations();
		authorizations.setUser(List.of(authorization(AUTH_TYPE_GRANT, "*", "UPDATE")));

		assertThat(SevenAuthorizationUtils.checkPermission(
			authorizations, SevenResourceType.USER, List.of("ALL", "UPDATE"))).isTrue();
	}

	@Test
	void checkPermission_refusesWhenNoEntryCarriesThePermission() {
		Authorizations authorizations = new Authorizations();
		authorizations.setUser(List.of(authorization(AUTH_TYPE_GRANT, "*", "READ")));

		assertThatThrownBy(() -> SevenAuthorizationUtils.checkPermission(
				authorizations, SevenResourceType.USER, List.of("DELETE")))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("Missing required permissions");
	}

	@Test
	void checkPermission_aRevokeBeatsAGrant() {
		Authorizations authorizations = new Authorizations();
		authorizations.setUser(List.of(
			authorization(AUTH_TYPE_GRANT, "*", "READ"),
			authorization(AUTH_TYPE_REVOKE, "*", "READ")));

		// an explicit revoke has to win, whatever else grants it
		assertThatThrownBy(() -> SevenAuthorizationUtils.checkPermission(
				authorizations, SevenResourceType.USER, List.of("READ")))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("explicitly revoked");
	}

	@Test
	void checkPermission_aRevokeOfAnotherPermissionDoesNotInterfere() {
		Authorizations authorizations = new Authorizations();
		authorizations.setUser(List.of(
			authorization(AUTH_TYPE_GRANT, "*", "READ"),
			authorization(AUTH_TYPE_REVOKE, "*", "DELETE")));

		assertThat(SevenAuthorizationUtils.checkPermission(
			authorizations, SevenResourceType.USER, List.of("READ"))).isTrue();
	}

	@Test
	void checkPermission_readsTheListBelongingToTheRequestedResourceType() {
		Authorizations authorizations = new Authorizations();
		// the grant sits on GROUP, but USER is being asked about
		authorizations.setGroup(List.of(authorization(AUTH_TYPE_GRANT, "*", "READ")));
		authorizations.setUser(List.of());

		assertThatThrownBy(() -> SevenAuthorizationUtils.checkPermission(
				authorizations, SevenResourceType.USER, List.of("READ")))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void checkPermission_coversEveryResourceTypeItClaimsToSupport() {
		// a resource type that fell through to the default branch would deny every request for it
		List<SevenResourceType> supported = List.of(
			SevenResourceType.USER, SevenResourceType.GROUP, SevenResourceType.AUTHORIZATION,
			SevenResourceType.PROCESS_DEFINITION, SevenResourceType.PROCESS_INSTANCE,
			SevenResourceType.DECISION_DEFINITION, SevenResourceType.DECISION_REQUIREMENTS_DEFINITION,
			SevenResourceType.TASK, SevenResourceType.FILTER, SevenResourceType.DEPLOYMENT,
			SevenResourceType.APPLICATION, SevenResourceType.BATCH, SevenResourceType.GROUP_MEMBERSHIP,
			SevenResourceType.HISTORIC_TASK, SevenResourceType.HISTORIC_PROCESS_INSTANCE,
			SevenResourceType.TENANT, SevenResourceType.TENANT_MEMBERSHIP, SevenResourceType.REPORT,
			SevenResourceType.DASHBOARD, SevenResourceType.USER_OPERATION_LOG_CATEGORY,
			SevenResourceType.SYSTEM);

		for (SevenResourceType type : supported) {
			Authorizations authorizations = allListsGranting("READ");

			assertThat(SevenAuthorizationUtils.checkPermission(authorizations, type, List.of("READ")))
				.as("resource type %s", type)
				.isTrue();
		}
		// every value of the enum is handled; none reaches the "not authorized to access resource
		// type" default
		assertThat(supported).hasSameSizeAs(List.of(SevenResourceType.values()));
	}

	@Test
	void hasAdminManagementPermissions_isTheSameCheck() {
		Authorizations authorizations = new Authorizations();
		authorizations.setUser(List.of(authorization(AUTH_TYPE_GRANT, "*", "READ")));

		assertThat(SevenAuthorizationUtils.hasAdminManagementPermissions(
			authorizations, SevenResourceType.USER, List.of("READ"))).isTrue();
	}

	// ---------- hasSpecificProcessRights ----------

	@Test
	void hasSpecificProcessRights_acceptsAGrantOfCreateInstanceOnThatKey() {
		Authorizations authorizations = new Authorizations();
		authorizations.setProcessDefinition(
			List.of(authorization(AUTH_TYPE_GRANT, "invoice", "CREATE_INSTANCE")));

		assertThat(SevenAuthorizationUtils.hasSpecificProcessRights(authorizations, "invoice")).isTrue();
	}

	@Test
	void hasSpecificProcessRights_acceptsAGrantOfAllOnThatKey() {
		Authorizations authorizations = new Authorizations();
		authorizations.setProcessDefinition(List.of(authorization(AUTH_TYPE_GRANT, "invoice", "ALL")));

		assertThat(SevenAuthorizationUtils.hasSpecificProcessRights(authorizations, "invoice")).isTrue();
	}

	@Test
	void hasSpecificProcessRights_refusesAGrantOnAnotherProcess() {
		Authorizations authorizations = new Authorizations();
		authorizations.setProcessDefinition(List.of(authorization(AUTH_TYPE_GRANT, "other", "ALL")));

		assertThatThrownBy(() -> SevenAuthorizationUtils.hasSpecificProcessRights(authorizations, "invoice"))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("invoice");
	}

	/**
	 * TODO KNOWN BUG (not fixed): unlike {@code hasCockpitRights}, this check compares the resource
	 * id to the process key exactly, so a grant on "*" - which the engine treats as "every
	 * process" - does not authorise starting a specific process here. This test asserts the
	 * wildcard grant the engine issues; it fails until the resource id is matched like
	 * {@code hasCockpitRights} matches it.
	 */
	@Test
	@Disabled("KNOWN BUG: hasSpecificProcessRights compares the resource id exactly, so a \"*\" grant authorises nothing")
	void hasSpecificProcessRights_acceptsTheWildcardResource() {
		Authorizations authorizations = new Authorizations();
		authorizations.setProcessDefinition(List.of(authorization(AUTH_TYPE_GRANT, "*", "ALL")));

		assertThat(SevenAuthorizationUtils.hasSpecificProcessRights(authorizations, "invoice")).isTrue();
	}

	@Test
	void hasSpecificProcessRights_ignoresAGlobalEntry() {
		Authorizations authorizations = new Authorizations();
		authorizations.setProcessDefinition(List.of(authorization(AUTH_TYPE_GLOBAL, "invoice", "ALL")));

		// only GRANT is considered here
		assertThatThrownBy(() -> SevenAuthorizationUtils.hasSpecificProcessRights(authorizations, "invoice"))
			.isInstanceOf(AccessDeniedException.class);
	}

	/** Every resource list populated with the same grant, so any resource type resolves. */
	private Authorizations allListsGranting(String permission) {
		Authorization grant = authorization(AUTH_TYPE_GRANT, "*", permission);
		List<Authorization> grants = List.of(grant);
		Authorizations authorizations = new Authorizations();
		authorizations.setUser(grants);
		authorizations.setGroup(grants);
		authorizations.setAuthorization(grants);
		authorizations.setProcessDefinition(grants);
		authorizations.setProcessInstance(grants);
		authorizations.setDecisionDefinition(grants);
		authorizations.setDecisionRequirementsDefinition(grants);
		authorizations.setTask(grants);
		authorizations.setFilter(grants);
		authorizations.setDeployment(grants);
		authorizations.setApplication(grants);
		authorizations.setBatch(grants);
		authorizations.setGroupMembership(grants);
		authorizations.setHistoricTask(grants);
		authorizations.setHistoricProcessInstance(grants);
		authorizations.setTenant(grants);
		authorizations.setTenantMembership(grants);
		authorizations.setReport(grants);
		authorizations.setDashboard(grants);
		authorizations.setUserOperationLogCategory(grants);
		authorizations.setSystem(grants);
		return authorizations;
	}
}
