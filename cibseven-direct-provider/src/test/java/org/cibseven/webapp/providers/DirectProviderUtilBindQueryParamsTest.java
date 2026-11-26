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
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.rest.dto.history.HistoricActivityInstanceQueryDto;
import org.cibseven.bpm.engine.rest.exception.InvalidRequestException;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.InvalidQueryParameterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;

/**
 * CIB7-2724: GET query parameters are bound to an engine query DTO through its
 * {@code (ObjectMapper, MultivaluedMap)} constructor, as engine-rest binds them.
 */
class DirectProviderUtilBindQueryParamsTest {

	private static final String ENGINE = "bindqueryparams";

	private DirectProviderUtil util;
	private ObjectMapper objectMapper;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		objectMapper = JacksonConfigurator.configureObjectMapper();
		util = new DirectProviderUtil();
		util.processEngines.put(ENGINE, mock(ProcessEngine.class));
		util.objectMappers.put(ENGINE, objectMapper);
		user = new CIBUser("demo");
		user.setEngine(ENGINE);
	}

	@Test
	void passesTheParametersAndTheEnginesObjectMapperToTheQueryParamConstructor() {
		Map<String, Object> params = new HashMap<>();
		params.put("name", "invoice");
		params.put("count", 2);

		RecordingDto dto = util.bindQueryParams(params, RecordingDto.class, user);

		assertThat(dto.objectMapper).isSameAs(objectMapper);
		assertThat(dto.queryParameters.getFirst("name")).isEqualTo("invoice");
		assertThat(dto.queryParameters.getFirst("count")).isEqualTo("2");
	}

	@Test
	void joinsTheElementsOfAListOrArrayWithCommasAsTheEnginesListConvertersExpect() {
		Map<String, Object> params = new HashMap<>();
		params.put("tenantIdIn", List.of("a", "b"));
		params.put("activityIdIn", new String[] { "task", "end" });
		params.put("processInstanceIdIn", Arrays.asList("1", null, "2"));

		RecordingDto dto = util.bindQueryParams(params, RecordingDto.class, user);

		assertThat(dto.queryParameters.get("tenantIdIn")).containsExactly("a,b");
		assertThat(dto.queryParameters.get("activityIdIn")).containsExactly("task,end");
		assertThat(dto.queryParameters.get("processInstanceIdIn")).containsExactly("1,2");
	}

	@Test
	void leavesOutEmptyAndBlankElementsOfAListOrArray() {
		Map<String, Object> params = new HashMap<>();
		params.put("tenantIdIn", List.of("a", "", " ", "b"));
		params.put("activityIdIn", new String[] { "task", "", "end" });
		params.put("processInstanceIdIn", List.of("", " "));

		RecordingDto dto = util.bindQueryParams(params, RecordingDto.class, user);

		assertThat(dto.queryParameters.get("tenantIdIn")).containsExactly("a,b");
		assertThat(dto.queryParameters.get("activityIdIn")).containsExactly("task,end");
		assertThat(dto.queryParameters).doesNotContainKey("processInstanceIdIn");
	}

	@Test
	void leavesOutNullValuesAndEmptyLists() {
		Map<String, Object> params = new HashMap<>();
		params.put("tenantId", null);
		params.put("tenantIdIn", List.of());
		params.put("activityIdIn", new String[0]);

		RecordingDto dto = util.bindQueryParams(params, RecordingDto.class, user);

		assertThat(dto.queryParameters).isEmpty();
	}

	@Test
	void bindsNoParametersWhenThereAreNone() {
		RecordingDto dto = util.bindQueryParams(null, RecordingDto.class, user);

		assertThat(dto.queryParameters).isEmpty();
	}

	@Test
	void answersAValueTheEnginesConverterRejectsWithABadRequest() {
		Map<String, Object> params = Map.of("startedAfter", "not a date");

		assertThatThrownBy(() -> util.bindQueryParams(params, HistoricActivityInstanceQueryDto.class, user))
			.isInstanceOf(InvalidQueryParameterException.class)
			.hasCauseInstanceOf(InvalidRequestException.class);
	}

	@Test
	void answersAnUnknownSortKeyWithABadRequest() {
		Map<String, Object> params = Map.of("sortBy", "noSuchField");

		assertThatThrownBy(() -> util.bindQueryParams(params, HistoricActivityInstanceQueryDto.class, user))
			.isInstanceOf(InvalidQueryParameterException.class)
			.hasMessageContaining("sortBy");
	}

	@Test
	void getOrBadRequestOnInvalidParameters_returnsTheResultOfTheStep() {
		assertThat(util.getOrBadRequestOnInvalidParameters(() -> "result")).isEqualTo("result");
	}

	@Test
	void getOrBadRequestOnInvalidParameters_answersTheEnginesBadRequestAsAnInvalidQueryParameter() {
		InvalidRequestException badRequest = new InvalidRequestException(Status.BAD_REQUEST, "sortBy and sortOrder required");

		assertThatThrownBy(() -> util.getOrBadRequestOnInvalidParameters(() -> { throw badRequest; }))
			.isInstanceOf(InvalidQueryParameterException.class)
			.hasMessage("sortBy and sortOrder required")
			.hasCause(badRequest);
	}

	@Test
	void getOrBadRequestOnInvalidParameters_passesOnARequestErrorThatIsNotABadRequest() {
		InvalidRequestException notFound = new InvalidRequestException(Status.NOT_FOUND, "no such thing");

		assertThatThrownBy(() -> util.getOrBadRequestOnInvalidParameters(() -> { throw notFound; }))
			.isSameAs(notFound);
	}

	@Test
	void getOrBadRequestOnInvalidParameters_passesOnAnyOtherException() {
		IllegalStateException failure = new IllegalStateException("broken");

		assertThatThrownBy(() -> util.getOrBadRequestOnInvalidParameters(() -> { throw failure; }))
			.isSameAs(failure);
	}

	@Test
	void rejectsADtoWithoutAQueryParamConstructor() {
		assertThatThrownBy(() -> util.bindQueryParams(Map.of(), String.class, user))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("java.lang.String")
			.hasMessageContaining("(ObjectMapper, MultivaluedMap)");
	}

	@Test
	void wrapsACheckedExceptionOfTheConstructor() {
		assertThatThrownBy(() -> util.bindQueryParams(Map.of(), CheckedFailingDto.class, user))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(CheckedFailingDto.class.getName())
			.hasCauseInstanceOf(Exception.class)
			.cause().hasMessage("checked");
	}

	@Test
	void wrapsADtoThatCannotBeInstantiated() {
		assertThatThrownBy(() -> util.bindQueryParams(Map.of(), AbstractDto.class, user))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(AbstractDto.class.getName())
			.hasCauseInstanceOf(InstantiationException.class);
	}

	public static class RecordingDto {
		final ObjectMapper objectMapper;
		final MultivaluedMap<String, String> queryParameters;

		public RecordingDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
			this.objectMapper = objectMapper;
			this.queryParameters = queryParameters;
		}
	}

	public static class CheckedFailingDto {
		public CheckedFailingDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) throws Exception {
			throw new Exception("checked");
		}
	}

	public abstract static class AbstractDto {
		public AbstractDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
		}
	}
}
