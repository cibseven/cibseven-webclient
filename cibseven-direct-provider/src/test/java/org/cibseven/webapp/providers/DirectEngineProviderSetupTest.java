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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineConfiguration;
import org.cibseven.bpm.engine.authorization.Authorization;
import org.cibseven.bpm.engine.authorization.Groups;
import org.cibseven.bpm.engine.authorization.Resources;
import org.cibseven.bpm.engine.impl.identity.Authentication;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.PasswordPolicyException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Credentials;
import org.cibseven.webapp.rest.model.Engine;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The first-run setup on a real engine, asked as the webclient asks it before the login: without a
 * user and without an X-Process-Engine header, so without an engine name (CIB7-2554). No
 * ProcessEngineProvider SPI is involved, as on Tomcat and WildFly.
 */
public class DirectEngineProviderSetupTest {

	private static final AtomicInteger DB_SEQUENCE = new AtomicInteger();

	private ProcessEngine engine;
	private DirectEngineProvider provider;

	@BeforeEach
	void setUp() {
		startEngine(false);
	}

	private void startEngine(boolean passwordPolicy) {
		if (engine != null)
			engine.close();
		String name = "setup" + DB_SEQUENCE.incrementAndGet();
		engine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
			.setJdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=1000")
			.setProcessEngineName(name)
			.setAuthorizationEnabled(true)
			.setEnablePasswordPolicy(passwordPolicy)
			.setJobExecutorActivate(false)
			.buildProcessEngine();

		DirectProviderUtil providerUtil = new DirectProviderUtil();
		providerUtil.processEngines.put(engine.getName(), engine);
		Engine definition = new Engine(engine.getName());
		definition.setId(engine.getName());
		provider = Mockito.spy(new DirectEngineProvider(providerUtil));
		doReturn(List.of(definition)).when(provider).getProcessEngineDefinitions();
		providerUtil.setEngineProvider(provider);
	}

	@AfterEach
	void tearDown() {
		engine.close();
	}

	@Test
	void requiresSetup_withoutEngineName_asksTheEffectiveDefaultEngine() {
		assertThat(provider.requiresSetup(null)).isTrue();
		assertThat(provider.requiresSetup("")).isTrue();
	}

	@Test
	void requiresSetup_withoutEngineName_isFalseOnceAnAdminGroupExists() {
		engine.getIdentityService().saveGroup(engine.getIdentityService().newGroup(Groups.CAMUNDA_ADMIN));

		assertThat(provider.requiresSetup(null)).isFalse();
	}

	@Test
	void createSetupUser_withoutEngineName_createsTheAdminOnTheEffectiveDefaultEngine() {
		provider.createSetupUser(newUser("admin"), null);

		assertThat(engine.getIdentityService().createUserQuery().userId("admin").count()).isEqualTo(1);
		assertThat(engine.getIdentityService().createUserQuery().memberOfGroup(Groups.CAMUNDA_ADMIN).userId("admin").count())
			.isEqualTo(1);
		assertThat(engine.getAuthorizationService().createAuthorizationQuery().groupIdIn(Groups.CAMUNDA_ADMIN)
			.resourceType(Resources.USER).resourceId(Authorization.ANY).count()).isEqualTo(1);
		assertThat(provider.requiresSetup(null)).isFalse();
	}

	@Test
	void createSetupUser_onceInitialized_isRefused() {
		provider.createSetupUser(newUser("admin"), null);

		assertThatThrownBy(() -> provider.createSetupUser(newUser("second"), null))
			.isInstanceOf(SystemException.class);
		assertThat(engine.getIdentityService().createUserQuery().userId("second").count()).isZero();
	}

	@Test
	void setup_restoresTheAuthenticationItFound() {
		engine.getIdentityService().setAuthentication("someone", List.of("someGroup"));

		provider.requiresSetup(null);

		Authentication restored = engine.getIdentityService().getCurrentAuthentication();
		assertThat(restored).isNotNull();
		assertThat(restored.getUserId()).isEqualTo("someone");
	}

	// --- only the effective default engine can be set up ---------------------------------------

	@Test
	void requiresSetup_namingTheDefaultEngine_asksIt() {
		assertThat(provider.requiresSetup(engine.getName())).isTrue();
	}

	/** Another name is not looked up: it would fail with "No process engine found" otherwise. */
	@Test
	void requiresSetup_namingAnotherEngine_isFalse() {
		assertThat(provider.requiresSetup("other")).isFalse();
	}

	@Test
	void createSetupUser_namingAnotherEngine_isRefused() {
		assertThatThrownBy(() -> provider.createSetupUser(newUser("admin"), "other"))
			.isInstanceOf(SystemException.class);
		assertThat(engine.getIdentityService().createUserQuery().count()).isZero();
		assertThat(provider.requiresSetup(null)).isTrue();
	}

	// --- one transaction ---------------------------------------------------------------------------

	/** The user exists already, so saving it fails: the admin group must not stay behind. */
	@Test
	void createSetupUser_failingHalfway_leavesNothingAndTheSetupOpen() {
		engine.getIdentityService().saveUser(engine.getIdentityService().newUser("admin"));

		assertThatThrownBy(() -> provider.createSetupUser(newUser("admin"), null));

		assertThat(engine.getIdentityService().createGroupQuery().groupId(Groups.CAMUNDA_ADMIN).count()).isZero();
		assertThat(engine.getAuthorizationService().createAuthorizationQuery().groupIdIn(Groups.CAMUNDA_ADMIN).count())
			.isZero();
		assertThat(provider.requiresSetup(null)).isTrue();

		provider.createSetupUser(newUser("admin2"), null);
		assertThat(engine.getIdentityService().createUserQuery().memberOfGroup(Groups.CAMUNDA_ADMIN).userId("admin2").count())
			.isEqualTo(1);
	}

	// --- concurrent setups -------------------------------------------------------------------------

	@Test
	void createSetupUser_concurrently_makesOnlyOneAdmin() throws Exception {
		int callers = 4;
		ExecutorService executor = Executors.newFixedThreadPool(callers);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<Boolean>> results = new ArrayList<>();
			for (int i = 0; i < callers; i++) {
				String id = "admin" + i;
				results.add(executor.submit(() -> {
					start.await();
					try {
						provider.createSetupUser(newUser(id), null);
						return true;
					} catch (RuntimeException e) {
						return false;
					}
				}));
			}
			start.countDown();
			int succeeded = 0;
			for (Future<Boolean> result : results)
				succeeded += result.get(60, TimeUnit.SECONDS) ? 1 : 0;

			assertThat(succeeded).isEqualTo(1);
			assertThat(engine.getIdentityService().createUserQuery().memberOfGroup(Groups.CAMUNDA_ADMIN).count())
				.isEqualTo(1);
			assertThat(engine.getIdentityService().createUserQuery().count()).isEqualTo(1);
		} finally {
			executor.shutdownNow();
		}
	}

	// --- the errors the setup page knows -----------------------------------------------------------

	@Test
	void createSetupUser_invalidId_throwsInvalidUserIdException() {
		assertThatThrownBy(() -> provider.createSetupUser(newUser("bad id!"), null))
			.isInstanceOf(InvalidUserIdException.class);
		assertThat(provider.requiresSetup(null)).isTrue();
	}

	@Test
	void createSetupUser_passwordAgainstThePolicy_throwsPasswordPolicyException() {
		startEngine(true);
		NewUser user = newUser("admin");
		user.getCredentials().setPassword("a");

		assertThatThrownBy(() -> provider.createSetupUser(user, null))
			.isInstanceOf(PasswordPolicyException.class);
		assertThat(provider.requiresSetup(null)).isTrue();
	}

	@Test
	void createSetupUser_withoutPassword_isRefused() {
		NewUser user = newUser("admin");
		user.getCredentials().setPassword(null);

		assertThatThrownBy(() -> provider.createSetupUser(user, null))
			.isInstanceOf(SystemException.class);
		assertThat(engine.getIdentityService().createUserQuery().count()).isZero();
	}

	private static NewUser newUser(String id) {
		User profile = new User();
		profile.setId(id);
		profile.setFirstName("First");
		profile.setLastName("Last");
		profile.setEmail(id + "@example.org");
		Credentials credentials = new Credentials("Passw0rd!");
		NewUser user = new NewUser();
		user.setProfile(profile);
		user.setCredentials(credentials);
		return user;
	}
}
