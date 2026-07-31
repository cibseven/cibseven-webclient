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
 * Verifies that a user may use the modeler, i.e. holds the application ACCESS permission that
 * also decides whether the modeler UI is shown.
 *
 * <p>Modeler diagrams, forms and element templates live in the webclient database rather than in
 * the engine, so no engine call can authorize them the way it does for tasks or process
 * instances. Without this check any authenticated user could read and write them through the REST
 * API even with the modeler hidden from them, and smuggle changes into a diagram that somebody
 * with deployment rights later deploys.</p>
 *
 * <p>Used by the modeler controllers through {@code ModelerBaseService} and by the STOMP
 * interceptor of the modeler chat.</p>
 */
@Slf4j
@Component
public class ModelerAccessChecker {

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

	public ModelerAccessChecker(BpmProvider bpmProvider,
			@Value("${camunda.bpm.authorization.enabled:true}") boolean legacyEngineAuthorizationEnabled,
			@Value("${cibseven.webclient.legacy.authorization.enabled:false}") boolean legacyAuthorizationEnabled) {
		this.bpmProvider = bpmProvider;
		this.legacyEngineAuthorizationEnabled = legacyEngineAuthorizationEnabled;
		this.legacyAuthorizationEnabled = legacyAuthorizationEnabled;
	}

	/**
	 * @throws AccessDeniedException if the user lacks the application ACCESS permission for the modeler
	 */
	public void checkModelerAccess(CIBUser user) {
		if (!isAuthorizationEnabled()) {
			return;
		}
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		if (!ModelerAuthorization.hasModelerAccess(authorizations)) {
			throw new AccessDeniedException("Access denied: Missing required permissions for modeler access. "
				+ "Required: ALL or ACCESS permission for application resource '"
				+ ModelerAuthorization.MODELER_RESOURCE_ID + "' or '*'");
		}
	}

	private boolean isAuthorizationEnabled() {
		Boolean resolved = authorizationEnabled;
		if (resolved == null) {
			// Not synchronized on purpose: concurrent callers may resolve the same value twice,
			// which is harmless, and the alternative would serialize every modeler request.
			EngineConfiguration engineConfig = bpmProvider.getEffectiveDefaultEngineConfiguration();
			if (engineConfig == null) {
				log.warn("engine-rest does not support the configuration endpoint, "
					+ "falling back to legacy configuration for the modeler authorization check");
			}
			resolved = (engineConfig == null ? legacyEngineAuthorizationEnabled : engineConfig.isAuthorizationEnabled())
				|| legacyAuthorizationEnabled;
			authorizationEnabled = resolved;
			log.info("Modeler authorization check is {}", resolved ? "enabled" : "disabled");
		}
		return resolved;
	}
}
