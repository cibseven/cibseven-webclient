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
package org.cibseven.modeler.rest;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.ModelerAccessChecker;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.rest.BaseService;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Base class of the controllers serving modeler data, providing the access check every one of
 * their endpoints has to perform. See {@link ModelerAccessChecker} for why it is needed.
 */
public abstract class ModelerBaseService extends BaseService {

	@Autowired
	protected ModelerAccessChecker modelerAccessChecker;

	/**
	 * Authenticates the caller and verifies that it may use the modeler.
	 *
	 * @return the calling user, never {@code null}
	 * @throws AccessDeniedException if the user lacks the application ACCESS permission for the
	 *         modeler, i.e. the permission that also decides whether the modeler UI is shown
	 */
	protected CIBUser checkModelerAccess(HttpServletRequest rq) {
		CIBUser user = checkModelerAuthentication(rq);
		checkModelerAccess(user);
		return user;
	}

	/**
	 * Verifies that the given, already authenticated user may use the modeler.
	 *
	 * @throws AccessDeniedException if the user lacks the application ACCESS permission for the modeler
	 */
	protected void checkModelerAccess(CIBUser user) {
		modelerAccessChecker.checkModelerAccess(user);
	}

	/**
	 * Authenticates the caller without requiring modeler rights. Reserved for the few endpoints
	 * that are also consumed outside the modeler; everything else has to use
	 * {@link #checkModelerAccess(HttpServletRequest)}.
	 *
	 * @return the calling user, never {@code null}
	 * @throws AuthenticationException if the caller could not be authenticated
	 */
	protected CIBUser checkModelerAuthentication(HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true);
		if (user == null) {
			// The providers throw on bad credentials, but the contract does not promise a user:
			// fail with 401 rather than dereferencing null further down.
			throw new AuthenticationException("Authentication required");
		}
		return user;
	}
}
