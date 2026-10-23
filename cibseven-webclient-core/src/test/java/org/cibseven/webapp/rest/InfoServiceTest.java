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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.EngineProvider;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
public class InfoServiceTest {

	@Mock
	private BpmProvider bpmProvider;

    private InfoService infoService;

	private SevenWebclientProperties properties;

	@BeforeEach
	public void setUp() {
		infoService = new InfoService();
		ReflectionTestUtils.setField(infoService, "bpmProvider", bpmProvider);

		properties = new SevenWebclientProperties();
		properties.getUser().setEditable(true);
		properties.getUser().setUserPasswordChangeEnabled(null);
		properties.getEngineRest().setUrl("./");
		infoService.properties = properties;
		ReflectionTestUtils.setField(infoService, "authorizationEnabled", true);
	}

	@Test
	public void testGetConfig_reportsWhetherPluginsAreEnabled() {
		EngineConfiguration config = new EngineConfiguration("default", "full", true, false);
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(config);

		// the frontend skips asking for plugins when this is false
		assertFalse(infoService.getConfig(null).get("pluginsEnabled").asBoolean());

		properties.getPlugins().setEnabled(true);
		assertTrue(infoService.getConfig(null).get("pluginsEnabled").asBoolean());
	}

	@Test
	public void testGetConfig_withNoEngineName_usesDefaultEngineConfiguration() {
		EngineConfiguration config = new EngineConfiguration("default", "full", true, false);
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(config);

		infoService.getConfig(null);

		verify(bpmProvider).getEffectiveDefaultEngineConfiguration();
		verify(bpmProvider, never()).getEngineConfiguration(any());
	}

	@Test
	public void testGetConfig_withEmptyEngineName_usesDefaultEngineConfiguration() {
		EngineConfiguration config = new EngineConfiguration("default", "full", true, false);
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(config);

		infoService.getConfig("");

		verify(bpmProvider).getEffectiveDefaultEngineConfiguration();
		verify(bpmProvider, never()).getEngineConfiguration(any());
	}

	@Test
	public void testGetConfig_withNoEngineName_picksEngineNamedDefaultAsEffectiveDefault() throws Exception {
		// End-to-end from InfoService.getConfig(null): the real EngineProvider lists the engines and,
		// because one is named "default", picks it as the effective default (served from the base /engine path).
		MockWebServer mockWebServer = new MockWebServer();
		mockWebServer.start();
		try {
			EngineProvider engineProvider = newEngineProvider(mockWebServer);
			when(bpmProvider.getEffectiveDefaultEngineConfiguration())
				.thenAnswer(invocation -> engineProvider.getEffectiveDefaultEngineConfiguration());

			mockWebServer.enqueue(new MockResponse()
					.setBody("[{\"name\":\"alpha\"},{\"name\":\"default\"}]")
					.addHeader("Content-Type", "application/json"));
			mockWebServer.enqueue(new MockResponse()
					.setBody("{\"engineName\":\"default\",\"historyLevel\":\"full\",\"authorizationEnabled\":true,\"enablePasswordPolicy\":false}")
					.addHeader("Content-Type", "application/json"));

			ObjectNode result = infoService.getConfig(null);

			assertEquals("/engine-rest/engine", mockWebServer.takeRequest().getPath());
			// The engine named "default" lives at the base /engine path, not at /engine/default.
			assertEquals("/engine-rest/configuration", mockWebServer.takeRequest().getPath());
			assertEquals("full", result.get("camundaHistoryLevel").asText());
		} finally {
			mockWebServer.shutdown();
		}
	}

	@Test
	public void testGetConfig_withNoEngineName_picksFirstEngineWhenNoneNamedDefault() throws Exception {
		// End-to-end from InfoService.getConfig(null): when no engine is named "default", the real
		// EngineProvider falls back to the first listed engine ("alpha") as the effective default.
		MockWebServer mockWebServer = new MockWebServer();
		mockWebServer.start();
		try {
			EngineProvider engineProvider = newEngineProvider(mockWebServer);
			when(bpmProvider.getEffectiveDefaultEngineConfiguration())
				.thenAnswer(invocation -> engineProvider.getEffectiveDefaultEngineConfiguration());

			mockWebServer.enqueue(new MockResponse()
					.setBody("[{\"name\":\"alpha\"},{\"name\":\"beta\"}]")
					.addHeader("Content-Type", "application/json"));
			mockWebServer.enqueue(new MockResponse()
					.setBody("{\"engineName\":\"alpha\",\"historyLevel\":\"audit\",\"authorizationEnabled\":true,\"enablePasswordPolicy\":false}")
					.addHeader("Content-Type", "application/json"));

			ObjectNode result = infoService.getConfig(null);

			assertEquals("/engine-rest/engine", mockWebServer.takeRequest().getPath());
			// The first engine ("alpha") is a named engine, so it is served from /engine/alpha.
			assertEquals("/engine-rest/engine/alpha/configuration", mockWebServer.takeRequest().getPath());
			assertEquals("audit", result.get("camundaHistoryLevel").asText());
		} finally {
			mockWebServer.shutdown();
		}
	}

	private EngineProvider newEngineProvider(MockWebServer mockWebServer) {
		EngineProvider engineProvider = new EngineProvider();
		ReflectionTestUtils.setField(engineProvider, "customRestTemplate", new CustomRestTemplate());
		((SevenWebclientProperties) ReflectionTestUtils.getField(engineProvider, "properties")).getEngineRest().setUrl("http://localhost:" + mockWebServer.getPort());
		return engineProvider;
	}

	@Test
	public void testGetConfig_withEngineName_usesNamedEngineConfiguration() {
		EngineConfiguration config = new EngineConfiguration("myEngine", "audit", true, false);
		when(bpmProvider.getEngineConfiguration("myEngine")).thenReturn(config);

		infoService.getConfig("myEngine");

		verify(bpmProvider).getEngineConfiguration("myEngine");
		verify(bpmProvider, never()).getEffectiveDefaultEngineConfiguration();
	}

	@Test
	public void testGetConfig_withLiteralDefaultEngineName_usesNamedEngineConfiguration() {
		// The engine literally named "default" is a specified engine, so it returns its own
		// configuration rather than the effective default.
		EngineConfiguration config = new EngineConfiguration("default", "full", true, false);
		when(bpmProvider.getEngineConfiguration("default")).thenReturn(config);

		infoService.getConfig("default");

		verify(bpmProvider).getEngineConfiguration("default");
		verify(bpmProvider, never()).getEffectiveDefaultEngineConfiguration();
	}

	@Test
	public void testGetConfig_withExternalEngine_usesNamedEngineConfiguration() {
		// An external "url|path|name" reference is a specified engine, so it returns its own
		// configuration rather than the effective default.
		String externalEngine = "http://other-host|/engine-rest|remote";
		EngineConfiguration config = new EngineConfiguration("remote", "audit", false, false);
		when(bpmProvider.getEngineConfiguration(externalEngine)).thenReturn(config);

		infoService.getConfig(externalEngine);

		verify(bpmProvider).getEngineConfiguration(externalEngine);
		verify(bpmProvider, never()).getEffectiveDefaultEngineConfiguration();
	}

	@Test
	public void testGetConfig_nullEngineConfig_fallsBackToLegacyConfiguration() {
		properties.setHistoryLevel("audit");
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(null);

		ObjectNode result = infoService.getConfig(null);

		assertEquals("audit", result.get("camundaHistoryLevel").asText());
	}

	@Test
	public void testGetConfig_historyLevelMappedToCamundaHistoryLevel() {
		EngineConfiguration config = new EngineConfiguration("default", "audit", true, false);
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(config);

		ObjectNode result = infoService.getConfig(null);

		assertEquals("audit", result.get("camundaHistoryLevel").asText());
	}

	@Test
	public void testGetConfig_authorizationEnabledFromEngineConfig() {
		EngineConfiguration config = new EngineConfiguration("default", "full", false, false);
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(config);
		properties.getLegacy().getAuthorization().setEnabled(false);

		ObjectNode result = infoService.getConfig(null);

		assertFalse(result.get("authorizationEnabled").asBoolean());
	}

	@Test
	public void testGetConfig_authorizationEnabled_trueWhenLegacyOverrides() {
		EngineConfiguration config = new EngineConfiguration("default", "full", false, false);
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(config);
		properties.getLegacy().getAuthorization().setEnabled(true);

		ObjectNode result = infoService.getConfig(null);

		assertTrue(result.get("authorizationEnabled").asBoolean());
	}

	@Test
	public void testGetConfig_passwordPolicyEnabledFromEngineConfig() {
		EngineConfiguration config = new EngineConfiguration("default", "full", true, true);
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(config);

		ObjectNode result = infoService.getConfig(null);

		assertNull(result.get("passwordPolicyEnabled"));
	}

	@Test
	public void testGetConfig_passwordPolicyDisabledFromEngineConfig() {
		EngineConfiguration config = new EngineConfiguration("default", "full", true, false);
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(config);

		ObjectNode result = infoService.getConfig(null);

		assertNull(result.get("passwordPolicyEnabled"));
	}

	@Test
	public void testGetConfig_notFoundException_fallsBackToLegacyHistoryLevel() {
		properties.setHistoryLevel("audit");
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(null);

		ObjectNode result = infoService.getConfig(null);

		assertEquals("audit", result.get("camundaHistoryLevel").asText());
	}

	@Test
	public void testGetConfig_notFoundException_fallsBackToLegacyAuthorizationEnabled() {
		ReflectionTestUtils.setField(infoService, "authorizationEnabled", false);
		when(bpmProvider.getEffectiveDefaultEngineConfiguration()).thenReturn(null);

		ObjectNode result = infoService.getConfig(null);

		assertFalse(result.get("authorizationEnabled").asBoolean());
	}

	@Test
	public void testGetConfig_notFoundExceptionForNamedEngine_fallsBackToLegacyConfiguration() {
		properties.setHistoryLevel("none");
		when(bpmProvider.getEngineConfiguration("myEngine")).thenReturn(null);

		ObjectNode result = infoService.getConfig("myEngine");

		assertEquals("none", result.get("camundaHistoryLevel").asText());
	}

	@Test
	public void testGetConfig_systemException_propagates() {
		when(bpmProvider.getEffectiveDefaultEngineConfiguration())
			.thenThrow(new SystemException("Engine unreachable"));

		assertThrows(SystemException.class, () -> infoService.getConfig(null));
	}
}
