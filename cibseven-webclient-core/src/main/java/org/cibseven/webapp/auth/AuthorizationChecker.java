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
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Answers whether a user holds a permission on a resource, for resources the engine cannot decide
 * about itself.
 *
 * <p>The webclient stores data of its own — modeler diagrams, forms, element templates, chat
 * messages — so for those no engine call can enforce anything and the check has to happen here. The
 * data comes from the engine all the same ({@link BpmProvider#getUserAuthorization}, which works
 * against a remote engine-rest as well as an embedded engine), and {@link AuthorizationCheck}
 * applies the engine's own grant/revoke rules to it.</p>
 *
 * <p>When the engine has authorization switched off, nothing is enforced: everything is authorized,
 * matching what the engine would answer and what the frontend was told through {@code InfoService}.</p>
 */
@Slf4j
@Component
public class AuthorizationChecker {

	private final BpmProvider bpmProvider;

	/**
	 * Legacy fallback used only with engine-rest versions prior to 2.2.0, which do not expose the
	 * engine configuration endpoint. Mirrors the resolution done in {@code InfoService} so that the
	 * backend enforces exactly what the frontend was told about authorization.
	 */
	private final boolean legacyEngineAuthorizationEnabled;

	private final boolean legacyAuthorizationEnabled;

	/**
	 * Resolved once: whether the engine has authorization enabled is fixed at engine startup, and
	 * resolving it costs an engine round-trip that would otherwise be paid on every request.
	 */
	private volatile Boolean authorizationEnabled;

	public AuthorizationChecker(BpmProvider bpmProvider,
			@Value("${camunda.bpm.authorization.enabled:true}") boolean legacyEngineAuthorizationEnabled,
			@Value("${cibseven.webclient.legacy.authorization.enabled:false}") boolean legacyAuthorizationEnabled) {
		this.bpmProvider = bpmProvider;
		this.legacyEngineAuthorizationEnabled = legacyEngineAuthorizationEnabled;
		this.legacyAuthorizationEnabled = legacyAuthorizationEnabled;
	}

	/**
	 * @return whether the user holds the permission, or {@code true} when the engine has
	 *         authorization disabled
	 */
	public boolean isAuthorized(CIBUser user, SevenResourceType resourceType, String resourceId, String permission) {
		if (!isAuthorizationEnabled()) {
			return true;
		}
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		return AuthorizationCheck.isAuthorized(authorizations, resourceType, resourceId, permission);
	}

	/**
	 * @throws AccessDeniedException if the user does not hold the permission
	 */
	public void requireAuthorized(CIBUser user, SevenResourceType resourceType, String resourceId, String permission) {
		if (!isAuthorized(user, resourceType, resourceId, permission)) {
			throw new AccessDeniedException("Access denied: Missing required permissions for "
				+ resourceType.name().toLowerCase() + " resource '" + resourceId + "'. Required: "
				+ AuthorizationCheck.PERMISSION_ALL + " or " + permission + " permission for '" + resourceId
				+ "' or '" + AuthorizationCheck.ANY_RESOURCE_ID + "'");
		}
	}

	/** Whether authorization is enforced at all, i.e. whether the engine has it enabled. */
	public boolean isAuthorizationEnabled() {
		Boolean resolved = authorizationEnabled;
		if (resolved == null) {
			// Not synchronized on purpose: concurrent callers may resolve the same value twice,
			// which is harmless, and the alternative would serialize every request.
			EngineConfiguration engineConfig = bpmProvider.getEffectiveDefaultEngineConfiguration();
			if (engineConfig == null) {
				log.warn("engine-rest does not support the configuration endpoint, "
					+ "falling back to legacy configuration for the authorization check");
			}
			resolved = (engineConfig == null ? legacyEngineAuthorizationEnabled : engineConfig.isAuthorizationEnabled())
				|| legacyAuthorizationEnabled;
			authorizationEnabled = resolved;
			log.info("Webclient authorization checks are {}", resolved ? "enabled" : "disabled");
		}
		return resolved;
	}
}
