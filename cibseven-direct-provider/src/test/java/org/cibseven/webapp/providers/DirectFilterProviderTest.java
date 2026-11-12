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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.List;

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

import tools.jackson.databind.JsonNode;

/**
 * The criteria of a filter are JSON trees in the webclient's model and typed query parameters in the
 * engine's; the direct provider converts between them with the engine's REST mapper.
 */
class DirectFilterProviderTest {

	private static final String CRITERIA = "{\"assigneeExpression\":\"${currentUser()}\",\"active\":true,"
			+ "\"processVariables\":[{\"name\":\"amount\",\"operator\":\"gt\",\"value\":100}],"
			+ "\"sorting\":[{\"sortBy\":\"created\",\"sortOrder\":\"desc\"}]}";

	private final CIBUser user = new CIBUser("demo");
	private DirectProviderUtil directProviderUtil;

	@BeforeEach
	void setUp() {
		directProviderUtil = Mockito.spy(new DirectProviderUtil());
		doReturn(JacksonConfigurator.configureObjectMapper()).when(directProviderUtil).getObjectMapper(any(CIBUser.class));
	}

	@Test
	void writesTheCriteriaOfAFilterForTheEngine() {
		Filter filter = new Filter("filter-1", "Task", "Mine", "demo",
				WebclientJsonMapper.plain().readValue(CRITERIA, FilterCriterias.class), null);

		FilterDto dto = directProviderUtil.convertValue(filter, FilterDto.class, user);

		TaskQueryDto query = (TaskQueryDto) dto.getQuery();
		assertThat(query.getAssigneeExpression()).isEqualTo("${currentUser()}");
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

		assertThat(filter.getQuery().getProcessVariables()).hasSize(1);
		JsonNode processVariable = filter.getQuery().getProcessVariables().get(0);
		assertThat(processVariable.get("name").asString()).isEqualTo("amount");
		assertThat(processVariable.get("operator").asString()).isEqualTo("lteq");
		assertThat(processVariable.get("value").longValue()).isEqualTo(12345678901L);
		assertThat(filter.getQuery().getSorting().get(0).get("sortBy").asString()).isEqualTo("dueDate");
		assertThat(filter.getQuery().getSorting().get(0).get("sortOrder").asString()).isEqualTo("asc");
	}
}
