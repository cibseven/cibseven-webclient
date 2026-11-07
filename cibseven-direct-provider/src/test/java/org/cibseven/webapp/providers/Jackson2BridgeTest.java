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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cibseven.bpm.engine.ManagementService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.impl.telemetry.dto.ApplicationServerImpl;
import org.cibseven.bpm.engine.impl.telemetry.dto.DatabaseImpl;
import org.cibseven.bpm.engine.impl.telemetry.dto.InternalsImpl;
import org.cibseven.bpm.engine.impl.telemetry.dto.JdkImpl;
import org.cibseven.bpm.engine.impl.telemetry.dto.ProductImpl;
import org.cibseven.bpm.engine.impl.telemetry.dto.TelemetryDataImpl;
import org.cibseven.bpm.engine.rest.dto.SortingDto;
import org.cibseven.bpm.engine.rest.dto.VariableQueryParameterDto;
import org.cibseven.bpm.engine.rest.dto.runtime.FilterDto;
import org.cibseven.bpm.engine.rest.dto.task.TaskQueryDto;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.config.WebclientJsonMapper;
import org.cibseven.webapp.rest.model.Filter;
import org.cibseven.webapp.rest.model.FilterCriterias;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

/**
 * The direct provider is still on Jackson 2, the rest of the webclient on Jackson 3 (CIB7-1535):
 * the trees of the webclient's models cross that border as JSON.
 */
class Jackson2BridgeTest {

	private static final String CRITERIA = "{\"assigneeExpression\":\"${currentUser()}\",\"active\":true,"
			+ "\"processVariables\":[{\"name\":\"amount\",\"operator\":\"gt\",\"value\":100}],"
			+ "\"sorting\":[{\"sortBy\":\"created\",\"sortOrder\":\"desc\"}]}";

	private final CIBUser user = new CIBUser("demo");
	private ObjectMapper engineMapper;
	private DirectProviderUtil directProviderUtil;

	@BeforeEach
	void setUp() {
		// The mapper DirectProviderUtil builds for an engine
		engineMapper = new ObjectMapper();
		JacksonConfigurator.configureObjectMapper(engineMapper);
		Jackson2Bridge.register(engineMapper);
		directProviderUtil = Mockito.spy(new DirectProviderUtil());
		doReturn(engineMapper).when(directProviderUtil).getObjectMapper(any(CIBUser.class));
	}

	@Test
	void writesTheCriteriaOfAFilterForTheEngine() {
		Filter filter = new Filter("filter-1", "Task", "Mine", "demo",
				WebclientJsonMapper.plain().readValue(CRITERIA, FilterCriterias.class), null);

		FilterDto dto = directProviderUtil.convertValue(filter, FilterDto.class, user);

		TaskQueryDto query = (TaskQueryDto) dto.getQuery();
		assertThat(query.getProcessVariables()).singleElement().satisfies(variable -> {
			assertThat(variable.getName()).isEqualTo("amount");
			assertThat(variable.getOperator()).isEqualTo("gt");
			assertThat(variable.getValue()).isEqualTo(100);
		});
		assertThat(query.getSorting()).singleElement().satisfies(sorting -> {
			assertThat(sorting.getSortBy()).isEqualTo("created");
			assertThat(sorting.getSortOrder()).isEqualTo("desc");
		});
	}

	@Test
	void readsTheCriteriaOfAnEngineFilter() {
		VariableQueryParameterDto variable = new VariableQueryParameterDto();
		variable.setName("amount");
		variable.setOperator("lteq");
		variable.setValue(12345678901L);
		SortingDto sorting = new SortingDto();
		sorting.setSortBy("dueDate");
		sorting.setSortOrder("asc");
		TaskQueryDto query = new TaskQueryDto();
		query.setProcessVariables(List.of(variable));
		query.setSorting(List.of(sorting));
		FilterDto dto = new FilterDto();
		dto.setId("filter-1");
		dto.setResourceType("Task");
		dto.setQuery(query);

		Filter filter = directProviderUtil.convertValue(dto, Filter.class, user);

		tools.jackson.databind.JsonNode processVariable = filter.getQuery().getProcessVariables().get(0);
		assertThat(filter.getQuery().getProcessVariables()).hasSize(1);
		assertThat(processVariable.get("name").asString()).isEqualTo("amount");
		assertThat(processVariable.get("operator").asString()).isEqualTo("lteq");
		assertThat(processVariable.get("value").longValue()).isEqualTo(12345678901L);
		assertThat(filter.getQuery().getSorting().get(0).get("sortBy").asString()).isEqualTo("dueDate");
		assertThat(filter.getQuery().getSorting().get(0).get("sortOrder").asString()).isEqualTo("asc");
	}

	@Test
	void rejectsAnObjectWhereTheModelExpectsAnArray() {
		assertThatThrownBy(() -> engineMapper.readValue("{\"processVariables\":{\"name\":\"amount\"}}", FilterCriterias.class))
				.isInstanceOf(MismatchedInputException.class)
				.hasMessageContaining("Expected a JSON array");
	}

	@Test
	void keepsNullAsNull() throws Exception {
		assertThat(Jackson2Bridge.toJackson3(null)).isNull();
		assertThat(Jackson2Bridge.toJackson2(null)).isNull();
		assertThat(engineMapper.readValue("{\"processVariables\":null}", FilterCriterias.class).getProcessVariables()).isNull();
	}

	@Test
	void keepsValuesAndOrderAcrossTheBorder() throws Exception {
		String json = "{\"z\":1,\"a\":12345678901,\"big\":123456789012345678901234567890,\"double\":1.5,"
				+ "\"text\":\"Größe – 😀\",\"null\":null,\"list\":[true,false,{}]}";

		tools.jackson.databind.JsonNode jackson3 = Jackson2Bridge.toJackson3(new ObjectMapper().readTree(json));

		assertThat(jackson3.toString()).isEqualTo(json);
		assertThat(Jackson2Bridge.toJackson2(jackson3).toString()).isEqualTo(json);
	}

	@Test
	void crossesTheBorderWithJackson2sNestingDepth() throws Exception {
		String nested = "[".repeat(1000) + "]".repeat(1000);

		tools.jackson.databind.JsonNode jackson3 = Jackson2Bridge.toJackson3(new ObjectMapper().readTree(nested));

		assertThat(WebclientJsonMapper.plain().writeValueAsString(jackson3)).isEqualTo(nested);
		assertThat(new ObjectMapper().writeValueAsString(Jackson2Bridge.toJackson2(jackson3))).isEqualTo(nested);
	}

	/** The interface hands the telemetry on as a Jackson 3 tree, which Spring MVC writes as JSON. */
	@Test
	void handsTheTelemetryOnAsJackson3Tree() throws Exception {
		ProcessEngine engine = mock(ProcessEngine.class);
		ManagementService managementService = mock(ManagementService.class);
		when(engine.getManagementService()).thenReturn(managementService);
		InternalsImpl internals = new InternalsImpl(new DatabaseImpl("h2", "2.4.240"),
				new ApplicationServerImpl("Apache Tomcat", "11.0.25"), null, new JdkImpl("17.0.12", "Oracle"));
		internals.setCommands(Map.of());
		internals.setMetrics(Map.of());
		internals.setWebapps(Set.of());
		internals.setCamundaIntegration(Set.of());
		internals.setDataCollectionStartDate(new Date(0));
		when(managementService.getTelemetryData())
				.thenReturn(new TelemetryDataImpl("8343cc7a", new ProductImpl("CIB seven", "3.0.0", "community", internals)));
		doReturn(engine).when(directProviderUtil).getProcessEngine(any(CIBUser.class));

		tools.jackson.databind.JsonNode telemetry = new DirectSystemProvider(directProviderUtil).getTelemetryData(user);

		assertThat(telemetry.get("installation").asString()).isEqualTo("8343cc7a");
		assertThat(telemetry.get("product").get("internals").get("database").get("vendor").asString()).isEqualTo("h2");
		// in the engine's date format, as engine-rest writes it
		assertThat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
				.parse(telemetry.get("product").get("internals").get("data-collection-start-date").asString()))
				.isEqualTo(new Date(0));
		assertThat(telemetry.toString()).isEqualTo(engineMapper.valueToTree(
				org.cibseven.bpm.engine.rest.dto.telemetry.TelemetryDataDto.fromEngineDto(
						managementService.getTelemetryData())).toString());
	}
}
