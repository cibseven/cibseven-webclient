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

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This type carries the answer of the engine's authorization check back to us, so the field name
 * has to match what the engine actually sends. Engine 2.2.0 answers {@code authorized}, while the
 * published REST reference documents {@code isAuthorized} — reading only one of them turns every
 * check into a silent denial.
 */
class AuthorizationCheckResultTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void readsTheFieldNameEngine220Sends() throws Exception {
		String response = "{\"permissionName\":\"ACCESS\",\"resourceName\":\"application\","
			+ "\"resourceId\":\"modeler\",\"authorized\":true}";

		assertTrue(objectMapper.readValue(response, AuthorizationCheckResult.class).isAuthorized());
	}

	@Test
	void readsTheFieldNameTheRestReferenceDocuments() throws Exception {
		String response = "{\"permissionName\":\"ACCESS\",\"resourceName\":\"application\","
			+ "\"resourceId\":\"modeler\",\"isAuthorized\":true}";

		assertTrue(objectMapper.readValue(response, AuthorizationCheckResult.class).isAuthorized());
	}

	@Test
	void aDenialIsNotMistakenForAGrant() throws Exception {
		String response = "{\"permissionName\":\"ACCESS\",\"resourceId\":\"modeler\",\"authorized\":false}";

		assertFalse(objectMapper.readValue(response, AuthorizationCheckResult.class).isAuthorized());
	}

	@Test
	void writesTheFieldNameTheRestReferenceDocuments() throws Exception {
		String json = objectMapper.writeValueAsString(
			new AuthorizationCheckResult("ACCESS", "application", "modeler", true));

		assertTrue(json.contains("\"isAuthorized\":true"), json);
	}
}
