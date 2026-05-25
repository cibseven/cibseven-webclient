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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.IEngineProvider;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
public class InfoServiceTest {

	@Mock
	private IEngineProvider engineProvider;

    private InfoService infoService;

	@BeforeEach
	public void setUp() {
		infoService = new InfoService();
		ReflectionTestUtils.setField(infoService, "engineProvider", engineProvider);

		// Set all @Value fields to defaults to avoid NullPointerExceptions
		ReflectionTestUtils.setField(infoService, "theme", "cib");
		ReflectionTestUtils.setField(infoService, "ssoActive", false);
		ReflectionTestUtils.setField(infoService, "authorizationEndpoint", "");
		ReflectionTestUtils.setField(infoService, "clientId", "");
		ReflectionTestUtils.setField(infoService, "scopes", "");
		ReflectionTestUtils.setField(infoService, "userProvider", "org.cibseven.webapp.auth.SevenUserProvider");
		ReflectionTestUtils.setField(infoService, "userEditable", true);
		ReflectionTestUtils.setField(infoService, "userPasswordChangeEnabled", null);
		ReflectionTestUtils.setField(infoService, "productNamePageTitle", "CIB seven");
		ReflectionTestUtils.setField(infoService, "servicesBasePath", "services/v1");
		ReflectionTestUtils.setField(infoService, "flowLinkTerms", "");
		ReflectionTestUtils.setField(infoService, "flowLinkPrivacy", "");
		ReflectionTestUtils.setField(infoService, "flowLinkImprint", "");
		ReflectionTestUtils.setField(infoService, "flowLinkAccessibility", "");
		ReflectionTestUtils.setField(infoService, "flowLinkHelp", "");
		ReflectionTestUtils.setField(infoService, "supportDialog", "");
		ReflectionTestUtils.setField(infoService, "engineRestPath", "/engine-rest");
		ReflectionTestUtils.setField(infoService, "engineRestUrl", "./");
		ReflectionTestUtils.setField(infoService, "legacyAuthorizationEnabled", false);
		ReflectionTestUtils.setField(infoService, "modelerEnabled", false);
		ReflectionTestUtils.setField(infoService, "camundaHistoryLevel", "full");
		ReflectionTestUtils.setField(infoService, "authorizationEnabled", true);
	}

	@Test
	public void testGetConfig_withNoEngineName_usesDefaultEngineConfiguration() {
		EngineConfiguration config = new EngineConfiguration("default", "full", true, false);
		when(engineProvider.getDefaultEngineConfiguration()).thenReturn(config);

		infoService.getConfig(null);

		verify(engineProvider).getDefaultEngineConfiguration();
		verify(engineProvider, never()).getEngineConfiguration(any());
	}

	@Test
	public void testGetConfig_withEmptyEngineName_usesDefaultEngineConfiguration() {
		EngineConfiguration config = new EngineConfiguration("default", "full", true, false);
		when(engineProvider.getDefaultEngineConfiguration()).thenReturn(config);

		infoService.getConfig("");

		verify(engineProvider).getDefaultEngineConfiguration();
		verify(engineProvider, never()).getEngineConfiguration(any());
	}

	@Test
	public void testGetConfig_withEngineName_usesNamedEngineConfiguration() {
		EngineConfiguration config = new EngineConfiguration("myEngine", "audit", true, false);
		when(engineProvider.getEngineConfiguration("myEngine")).thenReturn(config);

		infoService.getConfig("myEngine");

		verify(engineProvider).getEngineConfiguration("myEngine");
		verify(engineProvider, never()).getDefaultEngineConfiguration();
	}

	@Test
	public void testGetConfig_nullEngineConfig_throwsSystemException() {
		when(engineProvider.getDefaultEngineConfiguration()).thenReturn(null);

		assertThrows(SystemException.class, () -> infoService.getConfig(null));
	}

	@Test
	public void testGetConfig_historyLevelMappedToCamundaHistoryLevel() {
		EngineConfiguration config = new EngineConfiguration("default", "audit", true, false);
		when(engineProvider.getDefaultEngineConfiguration()).thenReturn(config);

		ObjectNode result = infoService.getConfig(null);

		assertEquals("audit", result.get("camundaHistoryLevel").asText());
	}

	@Test
	public void testGetConfig_authorizationEnabledFromEngineConfig() {
		EngineConfiguration config = new EngineConfiguration("default", "full", false, false);
		when(engineProvider.getDefaultEngineConfiguration()).thenReturn(config);
		ReflectionTestUtils.setField(infoService, "legacyAuthorizationEnabled", false);

		ObjectNode result = infoService.getConfig(null);

		assertFalse(result.get("authorizationEnabled").asBoolean());
	}

	@Test
	public void testGetConfig_authorizationEnabled_trueWhenLegacyOverrides() {
		EngineConfiguration config = new EngineConfiguration("default", "full", false, false);
		when(engineProvider.getDefaultEngineConfiguration()).thenReturn(config);
		ReflectionTestUtils.setField(infoService, "legacyAuthorizationEnabled", true);

		ObjectNode result = infoService.getConfig(null);

		assertTrue(result.get("authorizationEnabled").asBoolean());
	}

	@Test
	public void testGetConfig_passwordPolicyEnabledFromEngineConfig() {
		EngineConfiguration config = new EngineConfiguration("default", "full", true, true);
		when(engineProvider.getDefaultEngineConfiguration()).thenReturn(config);

		ObjectNode result = infoService.getConfig(null);

		assertNull(result.get("passwordPolicyEnabled"));
	}

	@Test
	public void testGetConfig_passwordPolicyDisabledFromEngineConfig() {
		EngineConfiguration config = new EngineConfiguration("default", "full", true, false);
		when(engineProvider.getDefaultEngineConfiguration()).thenReturn(config);

		ObjectNode result = infoService.getConfig(null);

		assertNull(result.get("passwordPolicyEnabled"));
	}

	@Test
	public void testGetConfig_notFoundException_fallsBackToLegacyHistoryLevel() {
		ReflectionTestUtils.setField(infoService, "camundaHistoryLevel", "audit");
		when(engineProvider.getDefaultEngineConfiguration())
			.thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));

		ObjectNode result = infoService.getConfig(null);

		assertEquals("audit", result.get("camundaHistoryLevel").asText());
	}

	@Test
	public void testGetConfig_notFoundException_fallsBackToLegacyAuthorizationEnabled() {
		ReflectionTestUtils.setField(infoService, "authorizationEnabled", false);
		when(engineProvider.getDefaultEngineConfiguration())
			.thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));

		ObjectNode result = infoService.getConfig(null);

		assertFalse(result.get("authorizationEnabled").asBoolean());
	}

	@Test
	public void testGetConfig_notFoundExceptionForNamedEngine_fallsBackToLegacyConfiguration() {
		ReflectionTestUtils.setField(infoService, "camundaHistoryLevel", "none");
		when(engineProvider.getEngineConfiguration("myEngine"))
			.thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));

		ObjectNode result = infoService.getConfig("myEngine");

		assertEquals("none", result.get("camundaHistoryLevel").asText());
	}

	@Test
	public void testGetConfig_restClientException_throwsSystemException() {
		when(engineProvider.getDefaultEngineConfiguration())
			.thenThrow(new RestClientException("Unexpected error"));

		assertThrows(SystemException.class, () -> infoService.getConfig(null));
	}
}
