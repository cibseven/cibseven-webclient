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

import org.cibseven.bpm.engine.AuthorizationException;
import org.cibseven.bpm.engine.IdentityService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineException;
import org.cibseven.bpm.engine.impl.identity.Authentication;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceQueryDto;
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

public class DirectProviderUtil {

	protected Map<String, ProcessEngine> processEngines = new HashMap<>();
	protected Map<String, ObjectMapper> objectMappers = new HashMap<>();

	public DirectProviderUtil() {
	}

	protected ProcessEngine getProcessEngine(String processEngineName) {
		ProcessEngine processEngine = null;
		if (processEngines.containsKey(processEngineName))
			processEngine = processEngines.get(processEngineName);
		else {
			processEngine = EngineUtil.lookupProcessEngine(processEngineName);
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
		processEngineName = "default";
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
