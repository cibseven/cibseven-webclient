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

import static org.mockito.Mockito.verify;

import java.util.Locale;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.BpmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** CIB7-2392: the decision view asks for one tenant's versions, or for those without a tenant. */
@ExtendWith(MockitoExtension.class)
class DecisionServiceVersionsTest {

	@Mock
	private BpmProvider bpmProvider;

	private final CIBUser user = new CIBUser("demo");
	private DecisionService decisionService;

	@BeforeEach
	void setUp() {
		decisionService = new DecisionService();
		ReflectionTestUtils.setField(decisionService, "bpmProvider", bpmProvider);
	}

	@Test
	void passesTheTenantOn() {
		decisionService.getDecisionVersionsByKey("risk", Optional.of("acme"), Optional.empty(), Optional.of(true), Locale.ENGLISH, user);

		verify(bpmProvider).getDecisionVersionsByKey("risk", "acme", false, Optional.of(true), user);
	}

	@Test
	void passesWithoutTenantOn() {
		decisionService.getDecisionVersionsByKey("risk", Optional.empty(), Optional.of(true), Optional.empty(), Locale.ENGLISH, user);

		verify(bpmProvider).getDecisionVersionsByKey("risk", null, true, Optional.empty(), user);
	}

	@Test
	void asksForEveryTenantsVersionsWithoutEither() {
		decisionService.getDecisionVersionsByKey("risk", Optional.empty(), Optional.empty(), Optional.empty(), Locale.ENGLISH, user);

		verify(bpmProvider).getDecisionVersionsByKey("risk", null, false, Optional.empty(), user);
	}

	@Test
	void treatsAnEmptyTenantAsNone() {
		decisionService.getDecisionVersionsByKey("risk", Optional.of(""), Optional.of(true), Optional.empty(), Locale.ENGLISH, user);

		verify(bpmProvider).getDecisionVersionsByKey("risk", null, true, Optional.empty(), user);
	}
}
