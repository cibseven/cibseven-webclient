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

import java.util.Arrays;
import java.util.Collection;

import org.cibseven.webapp.rest.model.Authorization;
import org.cibseven.webapp.rest.model.Authorizations;

/**
 * Evaluates whether a set of authorizations grants a permission on a specific resource.
 *
 * <p>The webclient owns resources the engine knows nothing about — modeler diagrams, forms and
 * element templates live in its own database — so it has to answer the authorization question
 * itself. This is that answer, for any resource type rather than one hardcoded case.</p>
 *
 * <p>The rules are the engine's, and match what the frontend evaluates in permissions.js: a grant
 * or global authorization for the exact resource id or for {@code *} allows the permission,
 * {@code ALL} covers every permission, and a revoke authorization for either resource id takes it
 * away again.</p>
 *
 * <p>Unlike the deprecated {@code SevenAuthorizationUtils#checkPermission}, this takes the resource
 * id into account: without it, ACCESS to the tasklist would satisfy a check for the modeler.</p>
 */
public final class AuthorizationCheck {

	/** Resource id matching any resource of a type. */
	public static final String ANY_RESOURCE_ID = "*";

	/** Permission covering every other permission. */
	public static final String PERMISSION_ALL = "ALL";

	private static final int AUTH_TYPE_GLOBAL = 0;
	private static final int AUTH_TYPE_GRANT = 1;
	private static final int AUTH_TYPE_REVOKE = 2;

	private AuthorizationCheck() {
	}

	/**
	 * @param authorizations the user's authorizations, as returned by the engine
	 * @param resourceType the resource type to check, e.g. {@code APPLICATION}
	 * @param resourceId the id of the resource, e.g. {@code modeler}
	 * @param permission the permission to check, e.g. {@code ACCESS}
	 * @return whether the permission is granted and not revoked
	 */
	public static boolean isAuthorized(Authorizations authorizations, SevenResourceType resourceType,
			String resourceId, String permission) {
		Collection<Authorization> candidates = authorizationsOf(authorizations, resourceType);
		if (candidates == null || candidates.isEmpty()) {
			return false;
		}
		// A revoke wins over any grant, exactly as in the frontend check.
		if (candidates.stream().anyMatch(auth -> auth.getType() == AUTH_TYPE_REVOKE
				&& covers(auth, resourceId, permission))) {
			return false;
		}
		return candidates.stream().anyMatch(auth ->
			(auth.getType() == AUTH_TYPE_GRANT || auth.getType() == AUTH_TYPE_GLOBAL)
				&& covers(auth, resourceId, permission));
	}

	private static boolean covers(Authorization auth, String resourceId, String permission) {
		return matchesResource(auth.getResourceId(), resourceId)
			&& hasPermission(auth.getPermissions(), permission);
	}

	private static boolean matchesResource(String granted, String requested) {
		return ANY_RESOURCE_ID.equals(granted) || (granted != null && granted.equals(requested));
	}

	private static boolean hasPermission(String[] granted, String requested) {
		return granted != null && Arrays.stream(granted)
			.anyMatch(permission -> PERMISSION_ALL.equals(permission) || permission.equals(requested));
	}

	private static Collection<Authorization> authorizationsOf(Authorizations authorizations,
			SevenResourceType resourceType) {
		if (authorizations == null || resourceType == null) {
			return null;
		}
		switch (resourceType) {
			case APPLICATION: return authorizations.getApplication();
			case USER: return authorizations.getUser();
			case GROUP: return authorizations.getGroup();
			case GROUP_MEMBERSHIP: return authorizations.getGroupMembership();
			case AUTHORIZATION: return authorizations.getAuthorization();
			case FILTER: return authorizations.getFilter();
			case PROCESS_DEFINITION: return authorizations.getProcessDefinition();
			case TASK: return authorizations.getTask();
			case PROCESS_INSTANCE: return authorizations.getProcessInstance();
			case DEPLOYMENT: return authorizations.getDeployment();
			case DECISION_DEFINITION: return authorizations.getDecisionDefinition();
			case DECISION_REQUIREMENTS_DEFINITION: return authorizations.getDecisionRequirementsDefinition();
			case TENANT: return authorizations.getTenant();
			case TENANT_MEMBERSHIP: return authorizations.getTenantMembership();
			case BATCH: return authorizations.getBatch();
			case REPORT: return authorizations.getReport();
			case DASHBOARD: return authorizations.getDashboard();
			case USER_OPERATION_LOG_CATEGORY: return authorizations.getUserOperationLogCategory();
			case HISTORIC_TASK: return authorizations.getHistoricTask();
			case HISTORIC_PROCESS_INSTANCE: return authorizations.getHistoricProcessInstance();
			case SYSTEM: return authorizations.getSystem();
			default: return null;
		}
	}
}
