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

import org.cibseven.webapp.rest.model.Authorizations;

/**
 * The modeler's case of {@link AuthorizationCheck}: the application ACCESS permission for the
 * {@code modeler} resource, the permission that also decides whether the modeler UI is shown.
 */
public final class ModelerAuthorization {

	/** Resource id of the modeler application, as used in the engine's application authorizations. */
	public static final String MODELER_RESOURCE_ID = ModelerAccessChecker.MODELER_RESOURCE_ID;

	private ModelerAuthorization() {
	}

	/**
	 * @param authorizations the authorizations of the user, as returned by the engine
	 * @return whether the user may use the modeler
	 */
	public static boolean hasModelerAccess(Authorizations authorizations) {
		return AuthorizationCheck.isAuthorized(authorizations, SevenResourceType.APPLICATION,
			MODELER_RESOURCE_ID, ModelerAccessChecker.MODELER_PERMISSION);
	}
}
