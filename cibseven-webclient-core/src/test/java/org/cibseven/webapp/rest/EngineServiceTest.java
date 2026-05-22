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
package org.cibseven.webapp.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.cibseven.webapp.providers.IEngineProvider;
import org.cibseven.webapp.rest.model.EngineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class EngineServiceTest {

	@Mock
	private IEngineProvider engineProvider;

	private EngineService engineService;

	@BeforeEach
	public void setUp() {
		engineService = new EngineService();
		ReflectionTestUtils.setField(engineService, "engineProvider", engineProvider);
	}

	@Test
	public void testGetDefaultEngineConfiguration_delegatesToEngineProvider() {
		EngineConfiguration expected = new EngineConfiguration("default", "full", true, false);
		when(engineProvider.getDefaultEngineConfiguration()).thenReturn(expected);

		EngineConfiguration result = engineService.getDefaultEngineConfiguration();

		assertEquals(expected, result);
		verify(engineProvider).getDefaultEngineConfiguration();
	}

	@Test
	public void testGetDefaultEngineConfiguration_returnsNullWhenProviderReturnsNull() {
		when(engineProvider.getDefaultEngineConfiguration()).thenReturn(null);

		EngineConfiguration result = engineService.getDefaultEngineConfiguration();

		assertNull(result);
		verify(engineProvider).getDefaultEngineConfiguration();
	}

	@Test
	public void testGetEngineConfiguration_delegatesToEngineProviderWithEngineName() {
		EngineConfiguration expected = new EngineConfiguration("myEngine", "audit", false, true);
		when(engineProvider.getEngineConfiguration("myEngine")).thenReturn(expected);

		EngineConfiguration result = engineService.getEngineConfiguration("myEngine");

		assertEquals(expected, result);
		verify(engineProvider).getEngineConfiguration("myEngine");
	}
}
