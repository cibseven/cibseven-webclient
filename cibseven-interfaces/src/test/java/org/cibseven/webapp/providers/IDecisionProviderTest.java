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
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Decision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CIB7-2392: an implementation that only lists every tenant's versions still answers the tenant
 * variant, by filtering that list.
 */
class IDecisionProviderTest {

	private final CIBUser user = new CIBUser("demo");
	private IDecisionProvider provider;

	private static Decision version(int number, String tenantId) {
		Decision decision = new Decision();
		decision.setVersion(number);
		decision.setTenantId(tenantId);
		return decision;
	}

	private final Decision withoutTenant = version(2, null);
	private final Decision ofAcme = version(1, "acme");
	private final Decision ofGlobex = version(1, "globex");

	@BeforeEach
	void setUp() {
		provider = mock(IDecisionProvider.class, CALLS_REAL_METHODS);
		doReturn(List.of(withoutTenant, ofAcme, ofGlobex)).when(provider).getDecisionVersionsByKey("risk", Optional.of(true), user);
	}

	@Test
	void keepsOneTenantsVersions() {
		assertThat(provider.getDecisionVersionsByKey("risk", "acme", false, Optional.of(true), user)).containsExactly(ofAcme);
	}

	@Test
	void keepsTheVersionsWithoutTenant() {
		assertThat(provider.getDecisionVersionsByKey("risk", null, true, Optional.of(true), user)).containsExactly(withoutTenant);
	}

	@Test
	void keepsEveryTenantsVersionsWithoutARestriction() {
		assertThat(provider.getDecisionVersionsByKey("risk", null, false, Optional.of(true), user))
			.containsExactly(withoutTenant, ofAcme, ofGlobex);
	}
}
