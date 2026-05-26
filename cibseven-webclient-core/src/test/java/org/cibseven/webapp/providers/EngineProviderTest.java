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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.cibseven.webapp.rest.CustomRestTemplate;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class EngineProviderTest {

	private static final String ENGINE_CONFIG_JSON =
			"{\"engineName\":\"default\",\"historyLevel\":\"full\",\"authorizationEnabled\":true,\"enablePasswordPolicy\":false}";

	private MockWebServer mockWebServer;
	private EngineProvider provider;

	@BeforeEach
	public void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();

		provider = new EngineProvider();
		ReflectionTestUtils.setField(provider, "customRestTemplate", new CustomRestTemplate());
		ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:" + mockWebServer.getPort());
		ReflectionTestUtils.setField(provider, "engineRestPath", "/engine-rest");
		ReflectionTestUtils.setField(provider, "jacksonParserMaxSize", 20_000_000);
	}

	@AfterEach
	public void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	public void testGetDefaultEngineConfiguration_callsCorrectUrl() throws InterruptedException {
		mockWebServer.enqueue(new MockResponse()
				.setBody(ENGINE_CONFIG_JSON)
				.addHeader("Content-Type", "application/json"));

		EngineConfiguration result = provider.getDefaultEngineConfiguration();

		RecordedRequest request = mockWebServer.takeRequest();
		assertEquals("/engine-rest/configuration", request.getPath());
		assertNotNull(result);
		assertEquals("full", result.getHistoryLevel());
		assertTrue(result.isAuthorizationEnabled());
		assertFalse(result.isEnablePasswordPolicy());
	}

	@Test
	public void testGetEngineConfiguration_withNullName_callsDefaultUrl() throws InterruptedException {
		mockWebServer.enqueue(new MockResponse()
				.setBody(ENGINE_CONFIG_JSON)
				.addHeader("Content-Type", "application/json"));

		provider.getEngineConfiguration(null);

		RecordedRequest request = mockWebServer.takeRequest();
		assertEquals("/engine-rest/configuration", request.getPath());
	}

	@Test
	public void testGetEngineConfiguration_withEmptyName_callsDefaultUrl() throws InterruptedException {
		mockWebServer.enqueue(new MockResponse()
				.setBody(ENGINE_CONFIG_JSON)
				.addHeader("Content-Type", "application/json"));

		provider.getEngineConfiguration("");

		RecordedRequest request = mockWebServer.takeRequest();
		assertEquals("/engine-rest/configuration", request.getPath());
	}

	@Test
	public void testGetEngineConfiguration_withDefaultName_callsDefaultUrl() throws InterruptedException {
		mockWebServer.enqueue(new MockResponse()
				.setBody(ENGINE_CONFIG_JSON)
				.addHeader("Content-Type", "application/json"));

		provider.getEngineConfiguration("default");

		RecordedRequest request = mockWebServer.takeRequest();
		assertEquals("/engine-rest/configuration", request.getPath());
	}

	@Test
	public void testGetEngineConfiguration_withEngineName_includesEngineInUrl() throws InterruptedException {
		String engineJson =
				"{\"engineName\":\"myEngine\",\"historyLevel\":\"audit\",\"authorizationEnabled\":false,\"enablePasswordPolicy\":true}";
		mockWebServer.enqueue(new MockResponse()
				.setBody(engineJson)
				.addHeader("Content-Type", "application/json"));

		EngineConfiguration result = provider.getEngineConfiguration("myEngine");

		RecordedRequest request = mockWebServer.takeRequest();
		assertEquals("/engine-rest/engine/myEngine/configuration", request.getPath());
		assertNotNull(result);
		assertEquals("audit", result.getHistoryLevel());
		assertFalse(result.isAuthorizationEnabled());
		assertTrue(result.isEnablePasswordPolicy());
	}

	@Test
	void testGetEngineConfiguration_notFound_returnsNull() {
		mockWebServer.enqueue(new MockResponse().setResponseCode(404));

		EngineConfiguration result = provider.getEngineConfiguration("default");

		assertNull(result);
	}

	@Test
	public void testRequiresSetup_withDefaultEngine_callsCorrectUrl() throws InterruptedException {
		mockWebServer.enqueue(new MockResponse()
				.setBody("false")
				.addHeader("Content-Type", "application/json"));

		Boolean result = provider.requiresSetup("default");

		RecordedRequest request = mockWebServer.takeRequest();
		assertEquals("/engine-rest/setup/status", request.getPath());
		assertFalse(result);
	}

	@Test
	public void testRequiresSetup_withNamedEngine_includesEngineInUrl() throws InterruptedException {
		mockWebServer.enqueue(new MockResponse()
				.setBody("true")
				.addHeader("Content-Type", "application/json"));

		Boolean result = provider.requiresSetup("myEngine");

		RecordedRequest request = mockWebServer.takeRequest();
		assertEquals("/engine-rest/engine/myEngine/setup/status", request.getPath());
		assertTrue(result);
	}
}
