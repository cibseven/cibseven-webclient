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

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.ProcessEngineException;
import org.cibseven.bpm.engine.rest.mapper.JacksonConfigurator;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Cannot convert variable decision")
			.hasMessageContaining("engine not ready");
	}
}
