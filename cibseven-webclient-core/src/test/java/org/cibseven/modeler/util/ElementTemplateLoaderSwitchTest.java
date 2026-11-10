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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.cibseven.modeler.repository.ElementTemplateRepository;
import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.providers.WebclientExtensions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The engine creates the element templates table, unless it has no webclient tables: then the
 * webclient does not wait for the table, nor logs an error for it (CIB7-2464). Loading runs after
 * the start, in a thread of its own, so the start never waits for the engine.
 */
class ElementTemplateLoaderSwitchTest {

	private final ElementTemplateRepository repository = mock(ElementTemplateRepository.class);
	private final ResourcePatternResolver resources = mock(ResourcePatternResolver.class);
	private final WebclientExtensions extensions = mock(WebclientExtensions.class);
	private final SevenWebclientProperties properties = new SevenWebclientProperties();
	private ElementTemplateLoader loader;

	@BeforeEach
	void setUp() {
		properties.getModeler().getTemplates().setPaths(List.of("classpath:templates/*.json"));
		properties.getModeler().getTemplates().setSchemaReadyMaxAttempts(30);
		properties.getModeler().getTemplates().setSchemaReadyRetryDelayMs(0);
		loader = new ElementTemplateLoader(repository, new ObjectMapper(), properties,
			mock(PlatformTransactionManager.class), resources, extensions);
	}

	@Test
	void doesNotWaitForTheTableWithoutTheEnginesWebclientTables() throws Exception {
		when(extensions.switchedOffNow()).thenReturn(true);

		loader.load();

		verify(repository, never()).count();
		verify(resources, never()).getResources(anyString());
	}

	/** The engine may only answer once it has started, which ends the wait there. */
	@Test
	void stopsWaitingOnceTheEngineReportsNoWebclientTables() throws Exception {
		when(extensions.switchedOffNow()).thenReturn(false, true);
		when(repository.count()).thenThrow(new IllegalStateException("MOD_ELEMENT_TEMPLATES not found"));

		loader.load();

		verify(repository, times(1)).count();
		verify(resources, never()).getResources(anyString());
	}

	@Test
	void waitsForTheTableWhileTheEngineDoesNotReportIt() throws Exception {
		when(repository.count()).thenThrow(new IllegalStateException("MOD_ELEMENT_TEMPLATES not found"));

		loader.load();

		verify(repository, times(30)).count();
		verify(resources, never()).getResources(anyString());
	}

	@Test
	void loadsOnceTheTableIsThere() throws Exception {
		when(repository.count()).thenReturn(0L);
		when(resources.getResources(anyString())).thenReturn(new Resource[0]);

		loader.load();

		verify(resources).getResources("classpath:templates/*.json");
	}

	@Test
	void loadsInAThreadOfItsOwnOnceTheApplicationIsReady() throws Exception {
		when(repository.count()).thenReturn(0L);
		when(resources.getResources(anyString())).thenReturn(new Resource[0]);

		loader.loadInBackground();

		verify(resources, timeout(5_000)).getResources("classpath:templates/*.json");
		loader.destroy();
	}

	/** A shutdown during the wait ends it, without the error of a table that never came. */
	@Test
	void stopsWaitingWhenTheWebclientStops() throws Exception {
		properties.getModeler().getTemplates().setSchemaReadyRetryDelayMs(60_000);
		when(repository.count()).thenThrow(new IllegalStateException("MOD_ELEMENT_TEMPLATES not found"));

		loader.loadInBackground();
		verify(repository, timeout(5_000)).count();
		loader.destroy();

		Thread loading = (Thread) org.springframework.test.util.ReflectionTestUtils.getField(loader, "loading");
		loading.join(5_000);
		assertThat(loading.isAlive()).isFalse();
		verify(resources, never()).getResources(anyString());
	}

	@Test
	void hasNothingToLoadWithoutPaths() throws Exception {
		properties.getModeler().getTemplates().setPaths(List.of());

		loader.loadInBackground();

		assertThat(org.springframework.test.util.ReflectionTestUtils.getField(loader, "loading")).isNull();
		loader.destroy();
	}
}
