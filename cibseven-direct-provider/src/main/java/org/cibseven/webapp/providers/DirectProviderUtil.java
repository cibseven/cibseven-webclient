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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.Function;

import org.cibseven.bpm.BpmPlatform;
import org.cibseven.bpm.engine.AuthorizationException;
import org.cibseven.bpm.engine.IdentityService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineException;
import org.cibseven.bpm.engine.identity.Group;
import org.cibseven.bpm.engine.identity.Tenant;
import org.cibseven.bpm.engine.impl.identity.Authentication;
import org.cibseven.bpm.engine.rest.dto.PatchVariablesDto;
import org.cibseven.bpm.engine.rest.dto.VariableValueDto;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceDto;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceQueryDto;
import org.cibseven.bpm.engine.rest.exception.InvalidRequestException;
import org.cibseven.bpm.engine.rest.exception.RestException;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.engine.rest.util.QueryUtil;
import org.cibseven.bpm.engine.runtime.VariableInstanceQuery;
import org.cibseven.bpm.engine.variable.VariableMap;
import org.cibseven.bpm.engine.variable.value.TypedValue;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.InvalidQueryParameterException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.cibseven.bpm.engine.rest.util.EngineUtil;
import org.cibseven.bpm.engine.rest.dto.AbstractQueryDto;

import org.cibseven.bpm.engine.query.Query;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;
import lombok.Setter;

public class DirectProviderUtil {

	private static final String FIRST_RESULT_PARAM = "firstResult";
	private static final String MAX_RESULTS_PARAM = "maxResults";

	protected Map<String, ProcessEngine> processEngines = new HashMap<>();
	protected Map<String, ObjectMapper> objectMappers = new HashMap<>();
	@Setter
	protected IEngineProvider engineProvider;

	protected ProcessEngine getProcessEngine(String engineId) {
		engineId = resolveEngineId(engineId);
		ProcessEngine processEngine = null;
		if (processEngines.containsKey(engineId))
			processEngine = processEngines.get(engineId);
		else {
			// either one of the two methods can be used to lookup the process engine - the other might fail for unknown reasons
			try {
				processEngine = EngineUtil.lookupProcessEngine(engineId);
			} catch (RestException ex) {
				processEngine = BpmPlatform.getProcessEngineService().getProcessEngine(engineId);
			} finally {
				if (processEngine == null)
					throw new SystemException("No process engine found with name " + engineId);
			}
			processEngines.put(engineId, processEngine);
			objectMappers.put(engineId, JacksonConfigurator.configureObjectMapper());
		}
		return processEngine;
	}

	protected ProcessEngine getProcessEngine(CIBUser user) {
		return getProcessEngine(getEngineId(user));
	}

	protected ObjectMapper getObjectMapper(CIBUser user) {
		String engineId = getEngineId(user);
		getProcessEngine(engineId);
		return objectMappers.get(engineId);
	}

	protected ObjectMapper getObjectMapper(String engineId) {
		engineId = resolveEngineId(engineId);
		getProcessEngine(engineId);
		return objectMappers.get(engineId);
	}

	protected String getEngineId(CIBUser user) {
		return resolveEngineId(user != null ? user.getEngine() : null);
	}

	/**
	 * An unspecified engine (a call without a user and without an X-Process-Engine header, such as
	 * the setup status asked before the login) resolves to the effective default engine. It must not
	 * be looked up by the name null: EngineUtil only maps null to the default engine through the
	 * ProcessEngineProvider SPI, which the webclient on Tomcat and WildFly does not have, and the
	 * BpmPlatform fallback finds no engine named null (CIB7-2554).
	 */
	protected String resolveEngineId(String engineId) {
		if (IEngineProvider.isEngineUnspecified(engineId) && engineProvider != null)
			return engineProvider.getEffectiveDefaultEngineId();
		return engineId;
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

	/**
	 * Converts a submitted form variable into the DTO the engine's {@code VariableValueDto.toMap}
	 * expects, applying the same adjustments {@code VariableProvider.submitVariables} makes before
	 * posting to engine-rest:
	 * <ul>
	 * <li>{@code json} and {@code Object} values that arrive as parsed JSON are serialized to a
	 * string, because the engine only accepts a string or {@code null} for serializable types</li>
	 * <li>{@code .rtf} files get the {@code application/rtf} mime type (BPM4CIB-434)</li>
	 * </ul>
	 */
	protected VariableValueDto toVariableValueDto(Variable variable, CIBUser user) {
		VariableValueDto variableValueDto = convertValue(variable, VariableValueDto.class, user);
		String type = variable.getType();
		Object value = variable.getValue();
		Map<String, Object> valueInfo = variable.getValueInfo() != null ? new HashMap<>(variable.getValueInfo()) : null;

		if (value != null && !(value instanceof String) && ("json".equalsIgnoreCase(type) || "Object".equalsIgnoreCase(type))) {
			try {
				value = getObjectMapper(user).writeValueAsString(value);
			} catch (JacksonException e) {
				throw new SystemException("Cannot serialize value of variable " + variable.getName(), e);
			}
		}

		//https://helpdesk.cib.de/browse/BPM4CIB-434
		String filename = valueInfo != null ? (String) valueInfo.get("filename") : null;
		if ("file".equalsIgnoreCase(type) && filename != null && filename.endsWith(".rtf") && filename.length() > 4) {
			valueInfo.put("mimeType", "application/rtf");
		}

		variableValueDto.setType(type);
		variableValueDto.setValue(value);
		if (valueInfo != null)
			variableValueDto.setValueInfo(valueInfo);
		return variableValueDto;
	}

	/** An engine-rest variable value (value, type, valueInfo) as a typed value, converted the way engine-rest converts it. */
	protected TypedValue toTypedValue(Map<String, Object> variable, String variableName, CIBUser user) {
		return converting("Cannot convert variable " + variableName, () -> getObjectMapper(user)
			.convertValue(variable, VariableValueDto.class)
			.toTypedValue(getProcessEngine(user), getObjectMapper(user)));
	}

	/** Engine-rest variable values by name as a variable map, converted the way engine-rest converts them. */
	protected VariableMap toVariableMap(Map<String, VariableValueDto> variables, String what, CIBUser user) {
		return converting("Cannot modify variables for " + what,
			() -> VariableValueDto.toMap(variables, getProcessEngine(user), getObjectMapper(user)));
	}

	/** An engine-rest variable patch body (modifications, deletions) as its DTO. */
	protected PatchVariablesDto toPatchVariables(Map<String, Object> patch, String what, CIBUser user) {
		return converting("Cannot modify variables for " + what,
			() -> getObjectMapper(user).convertValue(patch, PatchVariablesDto.class));
	}

	/** Every way the engine-rest conversions fail, reported as a SystemException. */
	private static <T> T converting(String failure, Supplier<T> conversion) {
		try {
			return conversion.get();
		} catch (RestException | IllegalArgumentException | JacksonException | ProcessEngineException e) {
			throw new SystemException(String.format("%s: %s", failure, e.getMessage()), e);
		}
	}

	/** Runs a variable write against the engine like the other variable writes: a missing permission is passed on, any other failure becomes a SystemException. */
	protected void writeVariables(String what, Runnable write) {
		try {
			write.run();
		} catch (AuthorizationException e) {
			throw e;
		} catch (ProcessEngineException e) {
			throw new SystemException(String.format("Cannot %s: %s", what, e.getMessage()), e);
		}
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
	 * Executes {@code action} with the given user authenticated on the engine, resolving the user's
	 * groups and tenants from the identity service. Where authorization is enabled, this is what lets
	 * the engine enforce its authorizations for the operation.
	 *
	 * <p>Set regardless of whether authorization is enabled: the engine also needs the acting user to
	 * write the user operation log and to resolve {@code ${currentUser()}} in filter expressions.
	 */
	protected <V extends Object> V runAsUser(CIBUser user, Supplier<V> action) {
		if (user == null || user.getId() == null) {
			return action.get();
		}
		IdentityService identityService = getProcessEngine(user).getIdentityService();
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

		// VariableHistory, not Variable: the ids (id, processInstanceId, executionId, taskId, ...) are only
		// mapped by the subclass and would otherwise be dropped from the response
		return new ArrayList<>(listAndConvert(query, firstResult, maxResults,
				VariableInstanceDto::fromVariableInstance, VariableHistory.class, user));
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

	protected <E, D, R> List<R> listAndConvert(Query<?, E> query, Integer firstResult, Integer maxResults,
			Function<E, D> toIntermediateDto, Class<R> targetClass, CIBUser user) {

		List<E> rawList = QueryUtil.list(query, firstResult, maxResults);
		List<R> result = new ArrayList<>(rawList.size());
		for (E item : rawList) {
			result.add(convertValue(toIntermediateDto.apply(item), targetClass, user));
		}
		return result;
	}

	/**
	 * Converts a JSON request body ({@code @RequestBody Map}) into an engine query DTO, the way engine-rest
	 * reads a POST query: through the no-arg constructor and Jackson. For GET query parameters use
	 * {@link #bindQueryParams(Map, Class, CIBUser)} instead.
	 */
	protected <Q extends AbstractQueryDto<?>> Q parseQueryDto(Object params, Class<Q> queryDtoClass, CIBUser user) {
		return getObjectMapper(user).convertValue(params, queryDtoClass);
	}

	/**
	 * Binds GET query parameters ({@code @RequestParam Map}) to an engine query or report DTO exactly as
	 * engine-rest does for the same GET request: through the DTO's {@code (ObjectMapper, MultivaluedMap)}
	 * constructor, which runs the {@code @CamundaQueryParam} converters for comma-separated {@code ...In}
	 * lists and case-insensitive enums.
	 * <p>
	 * For JSON request bodies use {@link #parseQueryDto} or {@code getObjectMapper(user).convertValue(...)}
	 * instead: a body carries real types, a query string only flat strings, and {@code convertValue} applies
	 * none of the converters - so a value such as {@code processDefinitionKeyIn=invoice,order} either fails
	 * or silently matches nothing (CIB7-2555, CIB7-2724). Dates are fine either way: the engine's
	 * {@code DateConverter} uses the same mapper.
	 * <p>
	 * A collection or array value is joined with commas, the separator the engine's list converters split on.
	 * Run the DTO's execute step ({@code toQuery}, {@code executeReport}) in
	 * {@link #getOrBadRequestOnInvalidParameters(Supplier)} as well: the engine checks some parameters only there.
	 *
	 * @throws InvalidQueryParameterException (400) when a converter rejects a value, as engine-rest answers
	 * @throws IllegalArgumentException when the DTO has no {@code (ObjectMapper, MultivaluedMap)} constructor
	 */
	public <T> T bindQueryParams(Map<String, Object> queryParams, Class<T> dtoClass, CIBUser user) {
		Constructor<T> constructor;
		try {
			constructor = dtoClass.getConstructor(ObjectMapper.class, MultivaluedMap.class);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(dtoClass.getName()
					+ " cannot be bound from query parameters: it has no (ObjectMapper, MultivaluedMap) constructor", e);
		}
		MultivaluedMap<String, String> multivaluedMap = toMultivaluedMap(queryParams != null ? queryParams : Map.of());
		ObjectMapper objectMapper = getObjectMapper(user);
		return getOrBadRequestOnInvalidParameters(() -> {
			try {
				return constructor.newInstance(objectMapper, multivaluedMap);
			} catch (InvocationTargetException e) {
				if (e.getCause() instanceof RuntimeException runtimeException)
					throw runtimeException;
				throw new IllegalStateException("Could not bind query parameters to " + dtoClass.getName(), e.getCause());
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Could not bind query parameters to " + dtoClass.getName(), e);
			}
		});
	}

	/**
	 * Returns the result of a step of an engine query DTO - binding it, or building the query from it
	 * ({@code toQuery}, {@code executeReport}) - and answers the engine's {@code InvalidRequestException}
	 * with status 400 as an {@link InvalidQueryParameterException}, which the ExceptionConverter answers
	 * with 400. Without this it would reach the ExceptionConverter as a plain RuntimeException and answer
	 * 500. Any other status, and any other exception, is passed on unchanged.
	 * <p>
	 * Engine-rest answers every {@code InvalidRequestException} with the status it carries, so this is the
	 * answer engine-rest gives for the same GET request. On these steps a 400 is always about a query
	 * parameter: an invalid {@code sortBy} or {@code sortOrder}, a value a converter cannot read, an invalid
	 * comparator, parameters that cannot be combined. Keep it that way: wrap only the binding and query
	 * building of query DTOs, not the execution of the query ({@code list}, {@code count}) and not request
	 * body DTOs, whose 400s are not about query parameters.
	 */
	public <R> R getOrBadRequestOnInvalidParameters(Supplier<R> step) {
		try {
			return step.get();
		} catch (InvalidRequestException e) {
			if (e.getStatus() == Status.BAD_REQUEST)
				throw new InvalidQueryParameterException(e.getMessage(), e);
			throw e;
		}
	}

	protected Integer getFirstResult(Map<String, Object> params) {
		return getPagingParam(params, FIRST_RESULT_PARAM);
	}

	protected Integer getMaxResults(Map<String, Object> params) {
		return getPagingParam(params, MAX_RESULTS_PARAM);
	}

	/**
	 * Copy of the request parameters without the paging keys, so that they are not
	 * offered to the query DTO as filter criteria.
	 */
	protected Map<String, Object> withoutPagingParams(Map<String, Object> params) {
		Map<String, Object> queryParams = new HashMap<>(params);
		queryParams.remove(FIRST_RESULT_PARAM);
		queryParams.remove(MAX_RESULTS_PARAM);
		return queryParams;
	}

	/**
	 * Values arrive as strings via {@code @RequestParam}, but internal callers may
	 * pass numbers.
	 */
	private Integer getPagingParam(Map<String, Object> params, String name) {
		Object value = params.get(name);
		if (value == null)
			return null;
		if (value instanceof Number)
			return ((Number) value).intValue();
		String text = value.toString().trim();
		return text.isEmpty() ? null : Integer.valueOf(text);
	}

	/**
	 * Holds the pagination parameters ({@code firstResult}/{@code maxResults}) extracted
	 * from a request parameter map, together with the remaining parameters as a
	 * {@link MultivaluedMap} suitable for building a query DTO.
	 */
	public static class PagedParams {
		private final Integer firstResult;
		private final Integer maxResults;
		private final MultivaluedMap<String, String> queryParams;

		PagedParams(Integer firstResult, Integer maxResults, MultivaluedMap<String, String> queryParams) {
			this.firstResult = firstResult;
			this.maxResults = maxResults;
			this.queryParams = queryParams;
		}

		public Integer getFirstResult() {
			return firstResult;
		}

		public Integer getMaxResults() {
			return maxResults;
		}

		public MultivaluedMap<String, String> getQueryParams() {
			return queryParams;
		}
	}

	/**
	 * Extracts {@code firstResult}/{@code maxResults} from the given params and collects
	 * the remaining entries into a {@link MultivaluedMap}.
	 */
	protected PagedParams extractPagedParams(Map<String, Object> params) {
		return new PagedParams(getFirstResult(params), getMaxResults(params),
				toMultivaluedMap(withoutPagingParams(params)));
	}

	/**
	 * Builds a {@link MultivaluedMap} containing all non-null entries of the given params.
	 * Null values are left out, as engine-rest never receives them; otherwise they would
	 * reach the query DTO as the filter value "null".
	 * <p>
	 * The engine DTO reads only the first value of each parameter, so the elements of a collection or
	 * array value are joined with a comma, which is what the engine's list converters split on. Null and
	 * blank elements are left out, so that no empty entry ({@code a,,b}) reaches the engine; a list without
	 * any other element is left out like a null value.
	 */
	protected MultivaluedMap<String, String> toMultivaluedMap(Map<String, Object> params) {
		MultivaluedMap<String, String> multiValueMap = new MultivaluedHashMap<>();
		for (Entry<String, Object> entry : params.entrySet()) {
			String value = toQueryParamValue(entry.getValue());
			if (value != null)
				multiValueMap.putSingle(entry.getKey(), value);
		}
		return multiValueMap;
	}

	private static String toQueryParamValue(Object value) {
		Stream<?> elements;
		if (value instanceof Collection<?> collection)
			elements = collection.stream();
		else if (value instanceof Object[] array)
			elements = Arrays.stream(array);
		else
			return value != null ? String.valueOf(value) : null;
		String joined = elements.filter(Objects::nonNull).map(String::valueOf)
				.filter(element -> !element.isBlank())
				.collect(Collectors.joining(","));
		return joined.isEmpty() ? null : joined;
	}
}
