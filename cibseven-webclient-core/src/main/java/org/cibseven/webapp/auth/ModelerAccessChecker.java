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

import org.cibseven.webapp.exception.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Verifies that a user may use the modeler, i.e. holds the application ACCESS permission that
 * also decides whether the modeler UI is shown.
 *
 * <p>Modeler diagrams, forms and element templates live in the webclient database rather than in
 * the engine, so no engine call can authorize them the way it does for tasks or process instances.
 * Without this check any authenticated user could read and write them through the REST API even
 * with the modeler hidden from them, and smuggle changes into a diagram that somebody with
 * deployment rights later deploys.</p>
 *
 * <p>This is the one modeler-specific place naming the resource and permission; the evaluation
 * itself is {@link AuthorizationChecker}, which serves every other resource too. Used by the
 * modeler controllers through {@code ModelerBaseService} and by the STOMP interceptor of the
 * modeler chat.</p>
 */
@Component
public class ModelerAccessChecker {

	/** Resource id of the modeler application, as used in the engine's application authorizations. */
	public static final String MODELER_RESOURCE_ID = "modeler";

	/** Permission the modeler requires on that resource. */
	public static final String MODELER_PERMISSION = "ACCESS";

	private final AuthorizationChecker authorizationChecker;

	public ModelerAccessChecker(AuthorizationChecker authorizationChecker) {
		this.authorizationChecker = authorizationChecker;
	}

	/**
	 * @throws AccessDeniedException if the user lacks the application ACCESS permission for the modeler
	 */
	public void checkModelerAccess(CIBUser user) {
		authorizationChecker.requireAuthorized(user, SevenResourceType.APPLICATION, MODELER_RESOURCE_ID,
			MODELER_PERMISSION);
	}
}
