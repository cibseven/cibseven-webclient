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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * The numeric type of each constant is sent to the engine's authorization API as
 * {@code resourceType}, so it has to agree with
 * {@code org.cibseven.bpm.engine.authorization.Resources} exactly. A silent drift here would not
 * fail anything - it would just check permissions against the wrong resource. The expectations
 * below were taken from that enum (verified against cibseven-engine 2.3.0).
 */
public class SevenResourceTypeTest {

	/** id -> name, as the engine defines it. */
	private static final Map<Integer, String> ENGINE_RESOURCE_TYPES = Map.ofEntries(
		Map.entry(0, "APPLICATION"),
		Map.entry(1, "USER"),
		Map.entry(2, "GROUP"),
		Map.entry(3, "GROUP_MEMBERSHIP"),
		Map.entry(4, "AUTHORIZATION"),
		Map.entry(5, "FILTER"),
		Map.entry(6, "PROCESS_DEFINITION"),
		Map.entry(7, "TASK"),
		Map.entry(8, "PROCESS_INSTANCE"),
		Map.entry(9, "DEPLOYMENT"),
		Map.entry(10, "DECISION_DEFINITION"),
		Map.entry(11, "TENANT"),
		Map.entry(12, "TENANT_MEMBERSHIP"),
		Map.entry(13, "BATCH"),
		Map.entry(14, "DECISION_REQUIREMENTS_DEFINITION"),
		Map.entry(15, "REPORT"),
		Map.entry(16, "DASHBOARD"),
		// the engine calls 17 OPERATION_LOG_CATEGORY; the webclient prefixes it with USER_
		Map.entry(17, "USER_OPERATION_LOG_CATEGORY"),
		// 18 is the engine's OPTIMIZE, which the webclient does not model
		Map.entry(19, "HISTORIC_TASK"),
		Map.entry(20, "HISTORIC_PROCESS_INSTANCE"),
		Map.entry(21, "SYSTEM"));

	@Test
	public void everyResourceTypeCarriesTheIdTheEngineExpects() {
		for (SevenResourceType type : SevenResourceType.values()) {
			String expectedName = ENGINE_RESOURCE_TYPES.get(type.getType());
			assertEquals(expectedName, type.name(),
				"resource type id " + type.getType() + " does not match the engine's resource of that id");
		}
	}

	@Test
	public void modelsEveryEngineResourceExceptOptimize() {
		Set<String> declared = Arrays.stream(SevenResourceType.values())
			.map(SevenResourceType::name)
			.collect(Collectors.toSet());

		assertEquals(new HashSet<>(ENGINE_RESOURCE_TYPES.values()), declared);
		// OPTIMIZE (18) is an engine-only resource, so the ids are deliberately not contiguous
		assertFalse(declared.contains("OPTIMIZE"));
	}

	@Test
	public void idsAreUnique() {
		Set<Integer> ids = Arrays.stream(SevenResourceType.values())
			.map(SevenResourceType::getType)
			.collect(Collectors.toSet());

		// A duplicate would make checkPermission() silently consult the wrong authorization list
		assertEquals(SevenResourceType.values().length, ids.size(), "duplicate resource type id");
		assertTrue(ids.contains(21));
		assertFalse(ids.contains(18));
	}
}
