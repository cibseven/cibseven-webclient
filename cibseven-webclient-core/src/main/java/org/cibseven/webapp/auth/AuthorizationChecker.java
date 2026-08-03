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
import org.cibseven.webapp.providers.BpmProvider;
import org.springframework.stereotype.Component;

/**
 * Answers whether a user holds a permission on a resource, for resources the engine cannot decide
 * about itself — modeler diagrams, forms, element templates and chat messages live in the
 * webclient's own database, so nothing in the engine enforces them.
 *
 * <p>The decision itself is the engine's: {@link BpmProvider#isUserAuthorized} asks it, over REST
 * or in-process depending on the provider. Evaluating the authorizations here instead was tried and
 * does not work — with authorization enabled a user needs READ on the authorization resource to see
 * its own authorizations, so an ordinary user's authorization list comes back empty and every check
 * would fail. The engine has no such problem, and it also answers true when authorization is
 * disabled, which is the behaviour the frontend was told about through {@code InfoService}.</p>
 */
@Component
public class AuthorizationChecker {

	private final BpmProvider bpmProvider;

	public AuthorizationChecker(BpmProvider bpmProvider) {
		this.bpmProvider = bpmProvider;
	}

	/**
	 * @return whether the user holds the permission, or {@code true} when the engine has
	 *         authorization disabled
	 */
	public boolean isAuthorized(CIBUser user, SevenResourceType resourceType, String resourceId, String permission) {
		return bpmProvider.isUserAuthorized(user, resourceType.getType(), resourceId, permission);
	}

	/**
	 * @throws AccessDeniedException if the user does not hold the permission
	 */
	public void requireAuthorized(CIBUser user, SevenResourceType resourceType, String resourceId, String permission) {
		if (!isAuthorized(user, resourceType, resourceId, permission)) {
			throw new AccessDeniedException("Access denied: Missing required permissions for "
				+ resourceType.name().toLowerCase() + " resource '" + resourceId + "'. Required: "
				+ permission + " permission for '" + resourceId + "'");
		}
	}
}
