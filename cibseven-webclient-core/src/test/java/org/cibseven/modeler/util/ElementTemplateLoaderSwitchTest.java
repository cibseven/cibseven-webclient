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
package org.cibseven.modeler.util;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.cibseven.modeler.repository.ElementTemplateRepository;
import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.providers.WebclientExtensions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The engine creates the element templates table, unless it has switched the webclient tables
 * off: then the start does not wait for the table, nor logs an error for it (CIB7-2464).
 */
class ElementTemplateLoaderSwitchTest {

	private final ElementTemplateRepository repository = mock(ElementTemplateRepository.class);
	private final ResourcePatternResolver resources = mock(ResourcePatternResolver.class);
	private final WebclientExtensions extensions = mock(WebclientExtensions.class);
	private ElementTemplateLoader loader;

	@BeforeEach
	void setUp() {
		SevenWebclientProperties properties = new SevenWebclientProperties();
		properties.getModeler().getTemplates().setPaths(List.of("classpath:templates/*.json"));
		properties.getModeler().getTemplates().setSchemaReadyMaxAttempts(30);
		properties.getModeler().getTemplates().setSchemaReadyRetryDelayMs(0);
		loader = new ElementTemplateLoader(repository, new ObjectMapper(), properties,
			mock(PlatformTransactionManager.class), resources, extensions);
	}

	@Test
	void doesNotWaitForTheTableOnceTheEngineHasSwitchedItOff() throws Exception {
		when(extensions.readEngineSwitch()).thenReturn(false);

		loader.afterPropertiesSet();

		verify(repository, never()).count();
		verify(resources, never()).getResources(anyString());
	}

	/** The engine may only answer once it has started, which ends the wait there. */
	@Test
	void stopsWaitingOnceTheEngineSaysItHasSwitchedTheTableOff() throws Exception {
		when(extensions.readEngineSwitch()).thenReturn(null, false);
		when(repository.count()).thenThrow(new IllegalStateException("MOD_ELEMENT_TEMPLATES not found"));

		loader.afterPropertiesSet();

		verify(repository, times(1)).count();
		verify(resources, never()).getResources(anyString());
	}

	@Test
	void waitsForTheTableWhileTheEngineDoesNotReportTheSwitch() throws Exception {
		when(extensions.readEngineSwitch()).thenReturn(null);
		when(repository.count()).thenThrow(new IllegalStateException("MOD_ELEMENT_TEMPLATES not found"));

		loader.afterPropertiesSet();

		verify(repository, times(30)).count();
		verify(resources, never()).getResources(anyString());
	}
}
