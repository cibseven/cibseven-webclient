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
package org.cibseven.webapp.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Map;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.LogoutRedirectProvider;
import org.cibseven.webapp.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

public class AuthenticationServiceTest {

	private BaseUserProvider<?> userProvider;
	private LogoutRedirectProvider redirectingProvider;
	private AuthenticationService service;

	@BeforeEach
	void setUp() {
		service = new AuthenticationService();
		useProvider(mock(BaseUserProvider.class, withSettings().extraInterfaces(LogoutRedirectProvider.class)));
		redirectingProvider = (LogoutRedirectProvider) userProvider;
	}

	private void useProvider(BaseUserProvider<?> provider) {
		userProvider = provider;
		ReflectionTestUtils.setField(service, "baseUserProvider", provider);
	}

	@Test
	void logout_handsTheProvidersLogoutRedirectUrlToTheBrowser() {
		User user = new CIBUser("demo");
		when(redirectingProvider.getLogoutRedirectUrl(user, "https://app/")).thenReturn("https://idp.example/logout?client_id=c");

		assertThat(service.logout(user, Map.of("returnUrl", "https://app/")))
			.containsEntry("logoutRedirectUrl", "https://idp.example/logout?client_id=c");
	}

	// The URL is built from what the logout may revoke
	@Test
	void logout_asksForTheRedirectUrlBeforeTheProviderLogsTheUserOut() {
		User user = new CIBUser("demo");

		service.logout(user, null);

		InOrder order = inOrder(userProvider, redirectingProvider);
		order.verify(redirectingProvider).getLogoutRedirectUrl(user, null);
		order.verify(userProvider).logout(user);
	}

	@Test
	void logout_answersNothingWhenTheProviderHasNothingToFinishExternally() {
		assertThat(service.logout(new CIBUser("demo"), Map.of())).isEmpty();
	}

	// Most providers have no external session; they just log the user out
	@Test
	void logout_stillLogsOutAProviderWithoutLogoutRedirect() {
		useProvider(mock(BaseUserProvider.class));
		User user = new CIBUser("demo");

		assertThat(service.logout(user, Map.of("returnUrl", "https://app/"))).isEmpty();

		verify(userProvider).logout(user);
	}
}
