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

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * @deprecated Use {@link OAuth2UserProvider} instead. This class is maintained for backward
 *             compatibility with existing configurations. Migration to OAuth2UserProvider is
 *             recommended.
 */
@Deprecated(since = "2.2.0", forRemoval = true)
@Slf4j
public class KeycloakUserProvider extends OAuth2UserProvider {
	
	@PostConstruct
	public void logDeprecation() {
		log.warn("KeycloakUserProvider is deprecated as of version 2.2.0 and will be removed in a future release. "
				+ "Please migrate to OAuth2UserProvider instead. The KeycloakUserProvider is maintained only for "
				+ "backward compatibility with existing configurations using the class name directly.");
	}
}