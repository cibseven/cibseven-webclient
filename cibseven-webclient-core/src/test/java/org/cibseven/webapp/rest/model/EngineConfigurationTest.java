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
package org.cibseven.webapp.rest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class EngineConfigurationTest {

	@Test
	public void testDefaultValues_engineNameIsNull() {
		EngineConfiguration config = new EngineConfiguration();
		assertNull(config.getEngineName());
	}

	@Test
	public void testDefaultValues_historyLevelIsFull() {
		EngineConfiguration config = new EngineConfiguration();
		assertEquals("full", config.getHistoryLevel());
	}

	@Test
	public void testDefaultValues_authorizationEnabledIsTrue() {
		EngineConfiguration config = new EngineConfiguration();
		assertTrue(config.isAuthorizationEnabled());
	}

	@Test
	public void testDefaultValues_enablePasswordPolicyIsFalse() {
		EngineConfiguration config = new EngineConfiguration();
		assertFalse(config.isEnablePasswordPolicy());
	}

	@Test
	public void testAllArgsConstructor_setsAllFields() {
		EngineConfiguration config = new EngineConfiguration("myEngine", "audit", false, true);
		assertEquals("myEngine", config.getEngineName());
		assertEquals("audit", config.getHistoryLevel());
		assertFalse(config.isAuthorizationEnabled());
		assertTrue(config.isEnablePasswordPolicy());
	}

	@Test
	public void testSetters_overrideValues() {
		EngineConfiguration config = new EngineConfiguration();
		config.setEngineName("test");
		config.setHistoryLevel("none");
		config.setAuthorizationEnabled(false);
		config.setEnablePasswordPolicy(true);

		assertEquals("test", config.getEngineName());
		assertEquals("none", config.getHistoryLevel());
		assertFalse(config.isAuthorizationEnabled());
		assertTrue(config.isEnablePasswordPolicy());
	}
}
