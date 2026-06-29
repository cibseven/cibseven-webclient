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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.cibseven.bpm.BpmPlatform;
import org.cibseven.bpm.engine.AuthorizationException;
import org.cibseven.bpm.engine.IdentityService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineException;
import org.cibseven.bpm.engine.identity.Group;
import org.cibseven.bpm.engine.identity.Tenant;
import org.cibseven.bpm.engine.impl.identity.Authentication;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceQueryDto;
import org.cibseven.bpm.engine.rest.exception.RestException;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.engine.rest.util.QueryUtil;
import org.cibseven.bpm.engine.runtime.VariableInstance;
import org.cibseven.bpm.engine.runtime.VariableInstanceQuery;
import org.cibseven.bpm.engine.variable.value.TypedValue;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.cibseven.bpm.engine.rest.util.EngineUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Setter;

public class DirectProviderUtil {

	protected Map<String, ProcessEngine> processEngines = new HashMap<>();
	protected Map<String, ObjectMapper> objectMappers = new HashMap<>();
	@Setter
	protected IEngineProvider engineProvider;

	protected ProcessEngine getProcessEngine(String processEngineName) {
		ProcessEngine processEngine = null;
		if (processEngines.containsKey(processEngineName))
			processEngine = processEngines.get(processEngineName);
		else {
			// either one of the two methods can be used to lookup the process engine - the other might fail for unknown reasons
			try {
				processEngine = EngineUtil.lookupProcessEngine(processEngineName);
			} catch (RestException ex) {
				processEngine = BpmPlatform.getProcessEngineService().getProcessEngine(processEngineName);
			} finally {
				if (processEngine == null)
					throw new SystemException("No process engine found with name " + processEngineName);
			}
			processEngines.put(processEngineName, processEngine);
			ObjectMapper objectMapper = new ObjectMapper();
			JacksonConfigurator.configureObjectMapper(objectMapper);
			objectMappers.put(processEngineName, objectMapper);
		}
		return processEngine;
	}

	protected ProcessEngine getProcessEngine(CIBUser user) {
		return getProcessEngine(getEngineName(user));
	}

	protected ObjectMapper getObjectMapper(CIBUser user) {
		String engineName = getEngineName(user);
		getProcessEngine(engineName);
		return objectMappers.get(engineName);
	}

	protected ObjectMapper getObjectMapper(String engineName) {
		getProcessEngine(engineName);
		return objectMappers.get(engineName);
	}

	protected String getEngineName(CIBUser user) {
		String processEngineName = user != null ? user.getEngine() : null;
		// If engine name is provided and not "default", add it to the path
		if (processEngineName == null || processEngineName.isEmpty())
			processEngineName = engineProvider.getEffectiveDefaultEngineName();
		return processEngineName;
	}

	/**
	 * conversion and helper functions
	 */
	protected <T> T convertValue(Object fromValueDto, Class<T> toValueType, CIBUser user) throws IllegalArgumentException {
		ObjectMapper objectMapper = getObjectMapper(user);
		Map<String, Object> filterDtoMap = objectMapper.convertValue(fromValueDto, new TypeReference<Map<String, Object>>() {
		});
		return objectMapper.convertValue(filterDtoMap, toValueType);
	}

	protected <V extends Object> V runWithoutAuthorization(Supplier<V> action, CIBUser user) {
		IdentityService identityService = getProcessEngine(user).getIdentityService();
		Authentication currentAuthentication = identityService.getCurrentAuthentication();
		try {
			identityService.clearAuthentication();
			return action.get();
		} finally {
			identityService.setAuthentication(currentAuthentication);
		}
	}

	/**
	 * Executes {@code action} with the given user authenticated on the engine, so that the engine
	 * enforces its authorizations for the operation. The user's groups and tenants are resolved from
	 * the engine's identity service.
	 *
	 * <p>If the engine has authorization disabled, the action runs unchanged: the engine performs
	 * no authorization checks, so setting the authentication (and the group/tenant identity queries it
	 * requires) would be pure overhead.
	 */
	protected <V extends Object> V runAsUser(CIBUser user, Supplier<V> action) {
		ProcessEngine processEngine = getProcessEngine(user);
		if (user == null || user.getId() == null
				|| !processEngine.getProcessEngineConfiguration().isAuthorizationEnabled()) {
			return action.get();
		}
		IdentityService identityService = processEngine.getIdentityService();
		Authentication previousAuthentication = identityService.getCurrentAuthentication();
		try {
			identityService.setAuthentication(user.getId(), getGroupsOfUser(user), getTenantsOfUser(user));
			return action.get();
		} finally {
			//Authentication set from another call will be restored
			identityService.setAuthentication(previousAuthentication);
		}
	}

	/**
	 * Void variant of {@link #runAsUser(CIBUser, Supplier)} for operations that do not return a value.
	 */
	protected void runAsUser(CIBUser user, Runnable action) {
		runAsUser(user, () -> {
			action.run();
			return null;
		});
	}

	protected List<String> getGroupsOfUser(CIBUser user) {
		List<Group> groups = getProcessEngine(user).getIdentityService().createGroupQuery()
				.groupMember(user.getId())
				.list();
		List<String> groupIds = new ArrayList<>();
		for (Group group : groups) {
			groupIds.add(group.getId());
		}
		return groupIds;
	}

	protected List<String> getTenantsOfUser(CIBUser user) {
		List<Tenant> tenants = getProcessEngine(user).getIdentityService().createTenantQuery()
				.userMember(user.getId())
				.includingGroupsOfUser(true)
				.list();
		List<String> tenantIds = new ArrayList<>();
		for (Tenant tenant : tenants) {
			tenantIds.add(tenant.getId());
		}
		return tenantIds;
	}

	public List<Variable> queryVariableInstances(VariableInstanceQueryDto queryDto, Integer firstResult,
			Integer maxResults, boolean deserializeObjectValues, CIBUser user) {
		VariableInstanceQuery query = queryDto.toQuery(getProcessEngine(user));

		// disable binary fetching by default.
		query.disableBinaryFetching();

		// disable custom object fetching by default. Cannot be done to not break
		// existing API
		if (!deserializeObjectValues) {
			query.disableCustomObjectDeserialization();
		}

		List<VariableInstance> matchingInstances = QueryUtil.list(query, firstResult,
				maxResults);

		List<Variable> instanceResults = new ArrayList<>();
		for (VariableInstance instance : matchingInstances) {
			VariableInstanceDto resultInstanceDto = VariableInstanceDto.fromVariableInstance(instance);
			VariableHistory resultInstance = convertValue(resultInstanceDto, VariableHistory.class, user);
			instanceResults.add(resultInstance);
		}
		return instanceResults;
	}

	public TypedValue getTypedValueForTaskVariable(String taskId, String variableName, boolean deserializeValue, CIBUser user) {
		TypedValue value = null;
		try {
			value = getProcessEngine(user).getTaskService().getVariableTyped(taskId, variableName, deserializeValue);
		} catch (AuthorizationException e) {
			throw e;
		} catch (ProcessEngineException e) {
			String errorMessage = String.format("Cannot get %s variable %s: %s", "task", variableName, e.getMessage());
			throw new SystemException(errorMessage, e);
		}

		if (value == null) {
			String errorMessage = String.format("%s variable with name %s does not exist", "task", variableName);
			throw new SystemException(errorMessage);
		}
		return value;
	}
}
