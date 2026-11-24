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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.cibseven.bpm.engine.AuthorizationException;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineConfiguration;
import org.cibseven.bpm.engine.ProcessEngineException;
import org.cibseven.bpm.engine.impl.variable.ValueTypeResolverImpl;
import org.cibseven.bpm.engine.rest.dto.VariableValueDto;
import org.cibseven.bpm.engine.rest.exception.RestException;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.InvalidRequestException;
import org.cibseven.webapp.exception.SystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response.Status;

class DirectProviderUtilVariableConversionTest {

	private final CIBUser user = new CIBUser("demo");
	private DirectProviderUtil util;
	private ProcessEngine engine;

	@BeforeEach
	void setUp() {
		util = spy(new DirectProviderUtil());
		engine = mock(ProcessEngine.class);
		doReturn(engine).when(util).getProcessEngine(any(CIBUser.class));
		doReturn(JacksonConfigurator.configureObjectMapper()).when(util).getObjectMapper(any(CIBUser.class));
	}

	@Test
	void toTypedValue_reportsAnEngineFailureDuringTheConversionAsSystemException() {
		// the conversion asks the engine for its value types; a failure there was wrapped before the refactor too
		when(engine.getProcessEngineConfiguration()).thenThrow(new ProcessEngineException("engine not ready"));

		assertThatThrownBy(() -> util.toTypedValue(Map.of("value", "x", "type", "String"), "decision", user))
			// a failure of the engine itself, which engine-rest answers with 500 as well
			.isExactlyInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot convert variable decision")
			.hasMessageContaining("engine not ready");
	}

	@Test
	void toVariableMap_reportsAnEngineFailureDuringTheConversionAsSystemException() {
		when(engine.getProcessEngineConfiguration()).thenThrow(new ProcessEngineException("engine not ready"));
		VariableValueDto value = new VariableValueDto();
		value.setValue("x");
		value.setType("String");

		assertThatThrownBy(() -> util.toVariableMap(Map.of("decision", value), "task task-1", user))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot modify variables for task task-1")
			.hasMessageContaining("engine not ready");
	}

	@Test
	void toPatchVariables_reportsABodyItCannotReadAsARejectedRequest() {
		assertThatThrownBy(() -> util.toPatchVariables(Map.of("modifications", "not a map"), "task task-1", user))
			.isInstanceOf(InvalidRequestException.class)
			// still a SystemException, for the callers catching the conversion failures as one
			.isInstanceOf(SystemException.class)
			.hasMessageStartingWith("Cannot modify variables for task task-1");
	}

	@Test
	void toTypedValue_reportsAValueOfTheWrongTypeAsARejectedRequest() {
		// engine-rest answers this with 400
		withTheEngineValueTypes();

		assertThatThrownBy(() -> util.toTypedValue(Map.of("value", "abc", "type", "Integer"), "amount", user))
			.isInstanceOf(InvalidRequestException.class)
			.hasMessageStartingWith("Cannot convert variable amount")
			.hasMessageContaining("abc");
	}

	@Test
	void toTypedValue_reportsAnUnknownTypeAsARejectedRequest() {
		withTheEngineValueTypes();

		assertThatThrownBy(() -> util.toTypedValue(Map.of("value", "x", "type", "NoSuchType"), "amount", user))
			.isInstanceOf(InvalidRequestException.class)
			.hasMessageStartingWith("Cannot convert variable amount");
	}

	@Test
	void toTypedValue_reportsAConversionFailureOtherThanABadRequestAsSystemException() {
		ProcessEngineConfiguration configuration = mock(ProcessEngineConfiguration.class);
		when(configuration.getValueTypeResolver()).thenThrow(new RestException(Status.INTERNAL_SERVER_ERROR, "resolver failed"));
		when(engine.getProcessEngineConfiguration()).thenReturn(configuration);

		assertThatThrownBy(() -> util.toTypedValue(Map.of("value", "x", "type", "String"), "amount", user))
			.isExactlyInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot convert variable amount: resolver failed");
	}

	private void withTheEngineValueTypes() {
		ProcessEngineConfiguration configuration = mock(ProcessEngineConfiguration.class);
		when(configuration.getValueTypeResolver()).thenReturn(new ValueTypeResolverImpl());
		when(engine.getProcessEngineConfiguration()).thenReturn(configuration);
	}

	@Test
	void writeVariables_passesAMissingPermissionOnLikeTheOtherVariableWrites() {
		AuthorizationException denied = new AuthorizationException("demo may not update the task");

		assertThatThrownBy(() -> util.writeVariables("modify variables of task task-1", () -> { throw denied; }))
			.isSameAs(denied);
	}

	@Test
	void writeVariables_reportsAnyOtherEngineFailureAsSystemException() {
		assertThatThrownBy(() -> util.writeVariables("modify variables of task task-1", () -> {
			throw new ProcessEngineException("task is gone");
		}))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot modify variables of task task-1: task is gone");
	}
}
