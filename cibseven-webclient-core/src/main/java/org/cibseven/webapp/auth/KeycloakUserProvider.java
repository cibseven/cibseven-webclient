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