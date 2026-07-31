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
 * Evaluates the application ACCESS permission for the "modeler" resource.
 *
 * <p>The modeler stores its diagrams and forms in the webclient database rather than in the
 * engine, so no engine call can authorize them: the permission that hides the modeler UI has to
 * be evaluated here as well, or the REST API stays open to every authenticated user.</p>
 *
 * <p>The rules mirror the frontend (permissions.js, {@code applicationPermissions(..., 'modeler')})
 * so that a user who is shown the modeler is also allowed to call it: a grant (or global)
 * authorization for resource id {@code modeler} or {@code *} carrying ACCESS or ALL grants
 * access, and a revoke authorization for either resource id takes it away again.</p>
 */
public final class ModelerAuthorization {

	/** Resource id of the modeler application, as used in the engine's application authorizations. */
	public static final String MODELER_RESOURCE_ID = "modeler";

	private static final String ANY_RESOURCE_ID = "*";

	private static final int AUTH_TYPE_GLOBAL = 0;
	private static final int AUTH_TYPE_GRANT = 1;
	private static final int AUTH_TYPE_REVOKE = 2;

	private static final String PERMISSION_ACCESS = "ACCESS";
	private static final String PERMISSION_ALL = "ALL";

	private ModelerAuthorization() {
	}

	/**
	 * @param authorizations the authorizations of the user, as returned by the engine
	 * @return whether the user may use the modeler
	 */
	public static boolean hasModelerAccess(Authorizations authorizations) {
		Collection<Authorization> application = authorizations == null ? null : authorizations.getApplication();
		if (application == null || application.isEmpty()) {
			return false;
		}
		// A revoke wins over any grant, exactly as in the frontend check.
		if (application.stream().anyMatch(auth -> auth.getType() == AUTH_TYPE_REVOKE && coversModelerAccess(auth))) {
			return false;
		}
		return application.stream().anyMatch(auth ->
			(auth.getType() == AUTH_TYPE_GRANT || auth.getType() == AUTH_TYPE_GLOBAL) && coversModelerAccess(auth));
	}

	private static boolean coversModelerAccess(Authorization auth) {
		return isModelerResource(auth.getResourceId()) && hasAccessPermission(auth.getPermissions());
	}

	private static boolean isModelerResource(String resourceId) {
		return MODELER_RESOURCE_ID.equals(resourceId) || ANY_RESOURCE_ID.equals(resourceId);
	}

	private static boolean hasAccessPermission(String[] permissions) {
		return permissions != null && Arrays.stream(permissions)
			.anyMatch(permission -> PERMISSION_ACCESS.equals(permission) || PERMISSION_ALL.equals(permission));
	}
}
