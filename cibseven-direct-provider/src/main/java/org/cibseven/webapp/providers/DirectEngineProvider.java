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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.cibseven.bpm.BpmPlatform;
import org.cibseven.bpm.engine.AuthorizationService;
import org.cibseven.bpm.engine.IdentityService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineException;
import org.cibseven.bpm.engine.authorization.Authorization;
import org.cibseven.bpm.engine.authorization.Groups;
import org.cibseven.bpm.engine.authorization.Permissions;
import org.cibseven.bpm.engine.authorization.Resource;
import org.cibseven.bpm.engine.authorization.Resources;
import org.cibseven.bpm.engine.identity.Group;
import org.cibseven.bpm.engine.identity.User;
import org.cibseven.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.cibseven.bpm.engine.impl.identity.Authentication;
import org.cibseven.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.cibseven.bpm.engine.impl.util.ParseUtil;
import org.cibseven.bpm.engine.rest.exception.RestException;
import org.cibseven.bpm.engine.rest.util.EngineUtil;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.PasswordPolicyException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Engine;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.cibseven.webapp.rest.model.NewUser;
import org.springframework.lang.Nullable;

public class DirectEngineProvider implements IEngineProvider {

	DirectProviderUtil directProviderUtil;

	private String effectiveDefaultEngineId = null;

	DirectEngineProvider(DirectProviderUtil directProviderUtil){
		this.directProviderUtil = directProviderUtil;
	}

	@Override
	public Collection<Engine> getProcessEngineDefinitions() {
		Set<String> engineNames = null;
		// either one of the two methods can be used to lookup the process engine - the other might fail for unknown reasons
		try {
			engineNames = EngineUtil.getProcessEngineProvider().getProcessEngineNames();
			if (engineNames == null || engineNames.isEmpty()) {
				engineNames = BpmPlatform.getProcessEngineService().getProcessEngineNames();
			}
		} catch (RestException ex) {
			engineNames = BpmPlatform.getProcessEngineService().getProcessEngineNames();
		} finally {
			if (engineNames == null || engineNames.isEmpty())
				throw new SystemException("No process engine found.");
		}
		List<Engine> results = new ArrayList<>();
		for (String engineName : engineNames) {
			Engine engine = new Engine(engineName);
			engine.setId(engineName);
			results.add(engine);
		}

		return results;
	}

	@Override
	@Nullable
	public EngineConfiguration getEngineConfiguration(String engineId) {
		// An unspecified engine resolves to the engine named "default", matching the HTTP provider.
		engineId = IEngineProvider.isEngineUnspecified(engineId) ? IEngineProvider.ENGINE_NAME_DEFAULT : engineId;
		org.cibseven.bpm.engine.ProcessEngine processEngine = directProviderUtil.getProcessEngine(engineId);
		if (processEngine == null) {
			return null;
		}
		org.cibseven.bpm.engine.ProcessEngineConfiguration config = processEngine.getProcessEngineConfiguration();
		if (config == null) {
			return null;
		}
		EngineConfiguration result = new EngineConfiguration();
		result.setEngineName(processEngine.getName());
		result.setHistoryLevel(config.getHistory());
		result.setAuthorizationEnabled(config.isAuthorizationEnabled());
		result.setEnablePasswordPolicy(config.isEnablePasswordPolicy());
		// getHistoryTimeToLive()/isEnforceHistoryTimeToLive() are declared on the impl class,
		// not the public ProcessEngineConfiguration interface.
		// For any other (custom) implementation the real values cannot be read, so both fields are
		// left null ("unknown") - the same as an engine-rest before 3.0.0 that does not report them -
		// instead of guessing the engine defaults.
		if (config instanceof org.cibseven.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl impl) {
			// Same representation as the engine-rest /configuration endpoint (3.0.0+):
			// "180" / "P180D" -> 180, "0" / "P0D" -> 0, null / "" (no default configured) -> -1.
			// The engine validates the value with the same method at startup, so it cannot throw here.
			Integer historyTimeToLive = ParseUtil.parseHistoryTimeToLive(impl.getHistoryTimeToLive());
			result.setHistoryTimeToLive(historyTimeToLive != null ? historyTimeToLive : -1);
			result.setEnforceHistoryTimeToLive(impl.isEnforceHistoryTimeToLive());
		}
		// What is in the database, as engine-rest reports it: the version of the webclient tables,
		// none when they are not there. An engine before 3.0.0 knows no such version, which leaves
		// it unknown rather than failing the whole configuration.
		try {
			result.setWebclientExtensionsEnabled(config.getWebclientExtensionsVersion() != null);
		} catch (LinkageError e) {
			result.setWebclientExtensionsEnabled(null);
		}
		return result;
	}

	@Override
	public String getEffectiveDefaultEngineId() {
		if (effectiveDefaultEngineId == null) {
			effectiveDefaultEngineId = IEngineProvider.super.getEffectiveDefaultEngineId();
		}
		return effectiveDefaultEngineId;
	}

	/*
	 * The setup works on the engine from directProviderUtil, which also finds it through the
	 * BpmPlatform, instead of on engine-rest's SetupRestServiceImpl: that one looks the engine up
	 * only through the ProcessEngineProvider SPI, which the webclient on Tomcat and WildFly does not
	 * have (CIB7-2554). What it checks and creates is the same as in SetupRestServiceImpl.
	 *
	 * Both calls are made without a login, so they are guarded more tightly than engine-rest's:
	 * - Only the effective default engine can be set up. The X-Process-Engine header comes from the
	 *   caller; otherwise anyone could claim the admin role on a further engine of an installation
	 *   that is already set up, and every name sent would be looked up.
	 * - The user, the admin group, its authorizations and the membership are created in one
	 *   transaction: a failure leaves nothing behind, so the setup stays open and can be repeated.
	 * - Concurrent setups are serialized in this JVM. Across cluster nodes the admin group's primary
	 *   key does it: both transactions insert it, the second one fails and is rolled back as a whole.
	 */

	private static final Object SETUP_LOCK = new Object();

	@Override
	public Boolean requiresSetup(String engineId) {
		if (!isSetupEngine(engineId)) {
			return false;
		}
		ProcessEngine processEngine = directProviderUtil.getProcessEngine(getEffectiveDefaultEngineId());
		if (processEngine.getIdentityService().isReadOnly()) {
			return false;
		}
		return runWithoutAuthorization(processEngine, () -> hasNoAdminGroup(processEngine));
	}

	@Override
	public void createSetupUser(NewUser user, String engineId) throws InvalidUserIdException {
		if (user.getProfile() == null || user.getProfile().getId() == null
			|| user.getCredentials() == null || user.getCredentials().getPassword() == null
			|| user.getCredentials().getPassword().isEmpty())
			throw new SystemException("User data not provided.");
		if (!isSetupEngine(engineId))
			throw new SystemException("Initial setup is only possible on the default engine.");
		ProcessEngine processEngine = directProviderUtil.getProcessEngine(getEffectiveDefaultEngineId());
		IdentityService identityService = processEngine.getIdentityService();
		if (identityService.isReadOnly())
			throw new SystemException("Identity service implementation is read-only.");
		synchronized (SETUP_LOCK) {
			try {
				runWithoutAuthorization(processEngine, () -> inOneTransaction(processEngine, () -> {
					// checked again here, in the same transaction as the creation
					if (!hasNoAdminGroup(processEngine))
						throw new SystemException("User creation not allowed - already initialized.");
					User newUser = identityService.newUser(user.getProfile().getId());
					newUser.setFirstName(user.getProfile().getFirstName());
					newUser.setLastName(user.getProfile().getLastName());
					newUser.setEmail(user.getProfile().getEmail());
					newUser.setPassword(user.getCredentials().getPassword());
					identityService.saveUser(newUser);
					createAdminGroup(processEngine);
					identityService.createMembership(newUser.getId(), Groups.CAMUNDA_ADMIN);
					return null;
				}));
			} catch (ProcessEngineException e) {
				throw translateSetupException(e);
			}
		}
	}

	/** No engine named, or the effective default engine named. */
	private boolean isSetupEngine(String engineId) {
		return IEngineProvider.isEngineUnspecified(engineId) || engineId.equals(getEffectiveDefaultEngineId());
	}

	private static boolean hasNoAdminGroup(ProcessEngine processEngine) {
		return processEngine.getIdentityService().createGroupQuery().groupId(Groups.CAMUNDA_ADMIN).count() == 0;
	}

	/** Only called after hasNoAdminGroup in the same transaction, so the group is always inserted. */
	private static void createAdminGroup(ProcessEngine processEngine) {
		IdentityService identityService = processEngine.getIdentityService();
		AuthorizationService authorizationService = processEngine.getAuthorizationService();
		Group adminGroup = identityService.newGroup(Groups.CAMUNDA_ADMIN);
		adminGroup.setName("camunda BPM Administrators");
		adminGroup.setType(Groups.GROUP_TYPE_SYSTEM);
		identityService.saveGroup(adminGroup);
		// ADMIN authorizations on all built-in resources
		for (Resource resource : Resources.values()) {
			if (authorizationService.createAuthorizationQuery().groupIdIn(Groups.CAMUNDA_ADMIN)
					.resourceType(resource).resourceId(Authorization.ANY).count() == 0) {
				AuthorizationEntity adminAuthorization = new AuthorizationEntity(Authorization.AUTH_TYPE_GRANT);
				adminAuthorization.setGroupId(Groups.CAMUNDA_ADMIN);
				adminAuthorization.setResource(resource);
				adminAuthorization.setResourceId(Authorization.ANY);
				adminAuthorization.addPermission(Permissions.ALL);
				authorizationService.saveAuthorization(adminAuthorization);
			}
		}
	}

	/**
	 * Runs the action as one engine command: the service calls inside it reuse its command context,
	 * so they share its transaction and are rolled back together.
	 */
	private static <V> V inOneTransaction(ProcessEngine processEngine, Supplier<V> action) {
		if (processEngine.getProcessEngineConfiguration() instanceof ProcessEngineConfigurationImpl config) {
			return config.getCommandExecutorTxRequired().execute(commandContext -> action.get());
		}
		return action.get();
	}

	/** The same exceptions the setup page gets from the HTTP provider (see SevenProviderBase). */
	private static RuntimeException translateSetupException(ProcessEngineException e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			String message = cause.getMessage();
			if (message == null)
				continue;
			if (message.contains("has an invalid id"))
				return new InvalidUserIdException(e);
			if (message.contains("Password does not match policy"))
				return new PasswordPolicyException(e);
		}
		return e;
	}


	private static <V> V runWithoutAuthorization(ProcessEngine processEngine, Supplier<V> action) {
		IdentityService identityService = processEngine.getIdentityService();
		Authentication currentAuthentication = identityService.getCurrentAuthentication();
		try {
			identityService.clearAuthentication();
			return action.get();
		} finally {
			identityService.setAuthentication(currentAuthentication);
		}
	}

}
