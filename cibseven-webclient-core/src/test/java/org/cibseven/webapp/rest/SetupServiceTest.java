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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.EngineProvider;
import org.cibseven.webapp.providers.IEngineProvider;
import org.cibseven.webapp.rest.model.Credentials;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The setup policy of SetupService, the same for every provider (CIB7-2554): the first-run setup
 * is only offered for the effective default engine, whatever the X-Process-Engine header names.
 */
public class SetupServiceTest {

	private static final String SEVEN_USER_PROVIDER = "org.cibseven.webapp.auth.SevenUserProvider";

	private static SetupService setupService(BpmProvider bpmProvider) {
		SetupService service = new SetupService();
		ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);
		SevenWebclientProperties properties = new SevenWebclientProperties();
		properties.getUser().setProvider(SEVEN_USER_PROVIDER);
		service.properties = properties;
		return service;
	}

	private static NewUser newUser() {
		return new NewUser(new User("admin", "First", "Last", "admin@example.org"), new Credentials("Passw0rd!"));
	}

	/** The policy itself, with any provider. */
	@Nested
	class Policy {

		private BpmProvider bpmProvider;
		private SetupService service;

		@BeforeEach
		void setUp() {
			IEngineProvider engineProvider = mock(IEngineProvider.class);
			when(engineProvider.getEffectiveDefaultEngineId()).thenReturn("main");
			bpmProvider = mock(BpmProvider.class);
			when(bpmProvider.getEngineProvider()).thenReturn(engineProvider);
			service = setupService(bpmProvider);
		}

		@Test
		void status_withoutHeader_asksTheEffectiveDefaultEngineByItsName() {
			when(bpmProvider.requiresSetup("main")).thenReturn(true);

			assertTrue(service.requiresSetup(null));
			assertTrue(service.requiresSetup(""));
		}

		@Test
		void status_namingTheEffectiveDefaultEngine_asksIt() {
			when(bpmProvider.requiresSetup("main")).thenReturn(true);

			assertTrue(service.requiresSetup("main"));
		}

		@Test
		void status_namingAnotherEngine_isFalseWithoutAskingIt() {
			assertFalse(service.requiresSetup("other"));
			verify(bpmProvider, never()).requiresSetup(anyString());
		}

		@Test
		void create_withoutHeader_createsTheAdminOnTheEffectiveDefaultEngine() {
			when(bpmProvider.requiresSetup("main")).thenReturn(true);
			NewUser user = newUser();

			assertEquals(HttpStatus.CREATED, service.createInitialUser(user, null).getStatusCode());
			verify(bpmProvider).createSetupUser(user, "main");
		}

		@Test
		void create_namingAnotherEngine_isForbiddenWithoutTouchingIt() {
			assertEquals(HttpStatus.FORBIDDEN, service.createInitialUser(newUser(), "other").getStatusCode());
			verify(bpmProvider, never()).requiresSetup(anyString());
			verify(bpmProvider, never()).createSetupUser(any(), any());
		}

		@Test
		void create_onceSetUp_isForbidden() {
			when(bpmProvider.requiresSetup("main")).thenReturn(false);

			assertEquals(HttpStatus.FORBIDDEN, service.createInitialUser(newUser(), null).getStatusCode());
			verify(bpmProvider, never()).createSetupUser(any(), any());
		}
	}

	/** The default provider: what reaches engine-rest. */
	@Nested
	class DefaultProvider {

		private MockWebServer engineRest;
		private SetupService service;

		@BeforeEach
		void setUp() throws IOException {
			engineRest = new MockWebServer();
			engineRest.start();
			EngineProvider engineProvider = new EngineProvider();
			ReflectionTestUtils.setField(engineProvider, "customRestTemplate", TestRestTemplateConfiguration.webclientCustomRestTemplate());
			SevenWebclientProperties providerProperties =
				(SevenWebclientProperties) ReflectionTestUtils.getField(engineProvider, "properties");
			providerProperties.getEngineRest().setUrl("http://localhost:" + engineRest.getPort());
			providerProperties.getEngineRest().setPath("/engine-rest");
			BpmProvider bpmProvider = mock(BpmProvider.class, Mockito.CALLS_REAL_METHODS);
			when(bpmProvider.getEngineProvider()).thenReturn(engineProvider);
			service = setupService(bpmProvider);
		}

		@AfterEach
		void tearDown() throws IOException {
			engineRest.shutdown();
		}

		private void engines(String json) {
			engineRest.enqueue(new MockResponse().setBody(json).addHeader("Content-Type", "application/json"));
		}

		private void setupStatus(boolean required) {
			engineRest.enqueue(new MockResponse().setBody(String.valueOf(required)).addHeader("Content-Type", "application/json"));
		}

		@Test
		void status_withoutHeader_asksTheEngineNamedDefault() throws InterruptedException {
			engines("[{\"name\":\"default\"},{\"name\":\"other\"}]");
			setupStatus(true);

			assertTrue(service.requiresSetup(null));

			assertEquals("/engine-rest/engine", engineRest.takeRequest().getPath());
			assertEquals("/engine-rest/setup/status", engineRest.takeRequest().getPath());
		}

		/** Without an engine named "default", the first engine is the effective default. */
		@Test
		void status_withoutHeaderAndWithoutDefaultEngine_asksTheFirstEngine() throws InterruptedException {
			engines("[{\"name\":\"beta\"},{\"name\":\"alpha\"}]");
			setupStatus(true);

			assertTrue(service.requiresSetup(null));

			assertEquals("/engine-rest/engine", engineRest.takeRequest().getPath());
			assertEquals("/engine-rest/engine/alpha/setup/status", engineRest.takeRequest().getPath());
		}

		@Test
		void status_namingAnotherEngine_isFalseWithoutAskingIt() throws InterruptedException {
			engines("[{\"name\":\"default\"},{\"name\":\"other\"}]");

			assertFalse(service.requiresSetup("other"));

			assertEquals("/engine-rest/engine", engineRest.takeRequest().getPath());
			RecordedRequest further = engineRest.takeRequest(200, TimeUnit.MILLISECONDS);
			assertEquals(null, further, "engine-rest was asked about the other engine");
		}

		@Test
		void create_namingAnotherEngine_isForbiddenWithoutPosting() throws InterruptedException {
			engines("[{\"name\":\"default\"},{\"name\":\"other\"}]");

			assertEquals(HttpStatus.FORBIDDEN, service.createInitialUser(newUser(), "other").getStatusCode());

			assertEquals("/engine-rest/engine", engineRest.takeRequest().getPath());
			assertEquals(null, engineRest.takeRequest(200, TimeUnit.MILLISECONDS), "engine-rest got a further request");
		}
	}
}
