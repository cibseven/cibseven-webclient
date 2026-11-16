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
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.rest.dto.runtime.VariableInstanceQueryDto;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.bpm.engine.runtime.VariableInstance;
import org.cibseven.bpm.engine.runtime.VariableInstanceQuery;
import org.cibseven.bpm.engine.variable.Variables;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DirectProviderUtilVariableQueryTest {

	private RuntimeService runtimeService;
	private DirectProviderUtil directProviderUtil;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		user = new CIBUser("testUser");

		ProcessEngine processEngine = mock(ProcessEngine.class);
		runtimeService = mock(RuntimeService.class);
		when(processEngine.getRuntimeService()).thenReturn(runtimeService);

		directProviderUtil = Mockito.spy(new DirectProviderUtil());
		doReturn(processEngine).when(directProviderUtil).getProcessEngine(any(CIBUser.class));
		doReturn(JacksonConfigurator.configureObjectMapper()).when(directProviderUtil).getObjectMapper(any(CIBUser.class));
	}

	@Test
	void queryVariableInstances_keepsTheIdsOfTheVariable() {
		VariableInstance instance = mock(VariableInstance.class);
		when(instance.getId()).thenReturn("var-1");
		when(instance.getName()).thenReturn("amount");
		when(instance.getTypedValue()).thenReturn(Variables.stringValue("42"));
		when(instance.getProcessInstanceId()).thenReturn("pi-1");
		when(instance.getExecutionId()).thenReturn("ex-1");
		when(instance.getTaskId()).thenReturn("task-1");
		when(instance.getActivityInstanceId()).thenReturn("act-1");

		VariableInstanceQuery query = mock(VariableInstanceQuery.class, withSettings().defaultAnswer(RETURNS_SELF));
		when(runtimeService.createVariableInstanceQuery()).thenReturn(query);
		when(query.list()).thenReturn(List.of(instance));

		List<Variable> variables = directProviderUtil.queryVariableInstances(new VariableInstanceQueryDto(), null, null, false, user);

		assertThat(variables).hasSize(1);
		assertThat(variables.get(0)).isInstanceOf(VariableHistory.class);
		VariableHistory variable = (VariableHistory) variables.get(0);
		assertThat(variable.getName()).isEqualTo("amount");
		assertThat(variable.getValue()).isEqualTo("42");
		assertThat(variable.getId()).isEqualTo("var-1");
		assertThat(variable.getProcessInstanceId()).isEqualTo("pi-1");
		assertThat(variable.getExecutionId()).isEqualTo("ex-1");
		assertThat(variable.getTaskId()).isEqualTo("task-1");
		assertThat(variable.getActivityInstanceId()).isEqualTo("act-1");
	}
}
