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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EngineConfigurationTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	// Reads a /configuration response whose historyTimeToLive is the given raw JSON value, with
	// properties after it, to check that those are still read whatever the TTL value looks like.
	private static EngineConfiguration readWithHistoryTimeToLive(String rawJsonValue) throws Exception {
		return MAPPER.readValue(
			"{\"engineName\":\"default\",\"historyTimeToLive\":" + rawJsonValue
				+ ",\"historyLevel\":\"audit\",\"authorizationEnabled\":false,\"enforceHistoryTimeToLive\":true}",
			EngineConfiguration.class);
	}

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
	public void testDefaultValues_historyTimeToLiveIsUnknown() {
		// null = "not reported by the engine" (before 3.0.0), never a guessed default such as -1
		EngineConfiguration config = new EngineConfiguration();
		assertNull(config.getHistoryTimeToLive());
	}

	@Test
	public void testDefaultValues_enforceHistoryTimeToLiveIsUnknown() {
		// null = "not reported by the engine" (before 3.0.0), never a guessed default such as true
		EngineConfiguration config = new EngineConfiguration();
		assertNull(config.getEnforceHistoryTimeToLive());
	}

	@Test
	public void testAllArgsConstructor_setsAllFields() {
		EngineConfiguration config = new EngineConfiguration("myEngine", "audit", false, true, 30, true);
		assertEquals("myEngine", config.getEngineName());
		assertEquals("audit", config.getHistoryLevel());
		assertFalse(config.isAuthorizationEnabled());
		assertTrue(config.isEnablePasswordPolicy());
		assertEquals(30, config.getHistoryTimeToLive());
		assertTrue(config.getEnforceHistoryTimeToLive());
	}

	@Test
	public void testSetters_overrideValues() {
		EngineConfiguration config = new EngineConfiguration();
		config.setEngineName("test");
		config.setHistoryLevel("none");
		config.setAuthorizationEnabled(false);
		config.setEnablePasswordPolicy(true);
		config.setHistoryTimeToLive(60);
		config.setEnforceHistoryTimeToLive(true);

		assertEquals("test", config.getEngineName());
		assertEquals("none", config.getHistoryLevel());
		assertFalse(config.isAuthorizationEnabled());
		assertTrue(config.isEnablePasswordPolicy());
		assertEquals(60, config.getHistoryTimeToLive());
		assertTrue(config.getEnforceHistoryTimeToLive());
	}

	@ParameterizedTest
	@CsvSource(value = {
		"FULL, full",
		"AUDIT, audit",
		"Audit, audit",
		"AuDiT, audit",
		"audit, audit",
		"Custom, custom",
		"null, full"
	}, nullValues = "null")
	public void testGetHistoryLevel_normalizesValue(String input, String expected) {
		EngineConfiguration config = new EngineConfiguration();
		config.setHistoryLevel(input);
		assertEquals(expected, config.getHistoryLevel());
	}

	// historyTimeToLive is reported differently by engine builds: a number (typed DTO, 3.0.0), the
	// raw configured string (builds in between, e.g. "P180D"), or not at all (before 3.0.0).

	@ParameterizedTest
	@CsvSource(value = {
		// typed DTO: JSON numbers, -1 = no default configured
		"180, 180",
		"0, 0",
		"-1, -1",
		// raw configured string: days or ISO-8601 period in days, like the engine accepts
		"'\"180\"', 180",
		"'\"P180D\"', 180",
		"'\"0\"', 0",
		"'\"P0D\"', 0",
		"'\" P30D \"', 30"
	})
	public void testDeserialize_historyTimeToLive_readsNumbersAndEngineStrings(String rawJsonValue, int expected) throws Exception {
		EngineConfiguration config = readWithHistoryTimeToLive(rawJsonValue);

		assertEquals(expected, config.getHistoryTimeToLive());
		assertEquals("audit", config.getHistoryLevel());
		assertTrue(config.getEnforceHistoryTimeToLive());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"null",
		// unparsable strings
		"\"\"", "\"  \"", "\"abc\"", "\"P180\"", "\"180D\"", "\"P1.5D\"", "\"1.5\"", "\"-5\"", "\"99999999999\"",
		// other JSON types
		"1.5", "99999999999", "true", "{\"days\":180}", "[180]"
	})
	public void testDeserialize_historyTimeToLive_unreadableValueIsUnknownAndKeepsTheRest(String rawJsonValue) throws Exception {
		// An unexpected value must never make the whole engine configuration unreadable: it is
		// read as null ("unknown"), and the properties after it are still read.
		EngineConfiguration config = readWithHistoryTimeToLive(rawJsonValue);

		assertNull(config.getHistoryTimeToLive());
		assertEquals("default", config.getEngineName());
		assertEquals("audit", config.getHistoryLevel());
		assertFalse(config.isAuthorizationEnabled());
		assertTrue(config.getEnforceHistoryTimeToLive());
	}

	@Test
	public void testDeserialize_historyTimeToLive_missingIsUnknown() throws Exception {
		EngineConfiguration config = MAPPER.readValue("{\"engineName\":\"default\"}", EngineConfiguration.class);

		assertNull(config.getHistoryTimeToLive());
		assertNull(config.getEnforceHistoryTimeToLive());
	}
}
