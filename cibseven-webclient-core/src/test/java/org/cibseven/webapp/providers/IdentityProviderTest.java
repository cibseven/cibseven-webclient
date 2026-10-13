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
package org.cibseven.webapp.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.cibseven.webapp.exception.EngineRestClientException;
import org.cibseven.webapp.rest.model.PasswordPolicyRequest;
import org.cibseven.webapp.rest.model.PasswordPolicyResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentityProviderTest {

	private MockEngineRest engine;
	private IdentityProvider provider;

	@BeforeEach
	void setUp() throws Exception {
		engine = new MockEngineRest();
		provider = engine.wire(new IdentityProvider());
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.close();
	}

	@Test
	void validatePasswordPolicy_returnsTheEngineVerdict() throws Exception {
		engine.enqueueJson("{\"valid\":false,\"rules\":[{\"placeholder\":\"PASSWORD_POLICY_LENGTH\"}]}");

		PasswordPolicyResponse response = provider.validatePasswordPolicy(request());

		assertThat(engine.takePath()).isEqualTo("/identity/password-policy");
		assertThat(response.isValid()).isFalse();
		assertThat(response.getRules()).hasSize(1);
	}

	/** With the password policy switched off the engine answers 404; every password is then valid. */
	@Test
	void validatePasswordPolicy_policyDisabled_acceptsThePassword() {
		engine.enqueueStatus(404, "{\"type\":\"InvalidRequestException\",\"message\":\"No password policy\"}");

		PasswordPolicyResponse response = provider.validatePasswordPolicy(request());

		assertThat(response.isValid()).isTrue();
		assertThat(response.getRules()).isEmpty();
	}

	@Test
	void validatePasswordPolicy_otherClientError_reachesTheCaller() {
		engine.enqueueStatus(400, "{\"type\":\"InvalidRequestException\",\"message\":\"bad profile\"}");

		assertThatThrownBy(() -> provider.validatePasswordPolicy(request()))
			.isInstanceOfSatisfying(EngineRestClientException.class, e -> {
				assertThat(e.getStatus()).isEqualTo(400);
				assertThat(e.getMessage()).isEqualTo("bad profile");
			});
	}

	private static PasswordPolicyRequest request() {
		return new PasswordPolicyRequest("secret",
			new PasswordPolicyRequest.Profile("demo", "Demo", "User", "demo@example.org"));
	}
}
