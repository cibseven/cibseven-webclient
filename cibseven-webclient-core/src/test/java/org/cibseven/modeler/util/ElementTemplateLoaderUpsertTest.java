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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.cibseven.modeler.model.ElementTemplate;
import org.cibseven.modeler.model.ElementTemplateOrigin;
import org.cibseven.modeler.repository.ElementTemplateRepository;
import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.providers.WebclientExtensions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * A restart brings the stored templates up to their source only where no user has touched them
 * (CIB7-2704): what users created, edited, hid or showed stays as they left it.
 */
class ElementTemplateLoaderUpsertTest {

	private static final String SOURCE = "[{\"id\":\"io.cibseven.mail\",\"name\":\"Mail 2\",\"description\":\"From the source\"}]";

	private final ElementTemplateRepository repository = mock(ElementTemplateRepository.class);
	private final ResourcePatternResolver resources = mock(ResourcePatternResolver.class);

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " " })
	void bringsAnUntouchedTemplateUpToItsSourceAndKeepsItsVisibility(String nobody) throws Exception {
		ElementTemplate stored = stored(ElementTemplateOrigin.DEFAULT_JSON);
		stored.setActive(false);
		stored.setCreatedBy(nobody);
		stored.setUpdatedBy(nobody);

		load();

		verify(repository).save(stored);
		assertThat(stored.getId()).isEqualTo("db-1");
		assertThat(stored.getName()).isEqualTo("Mail 2");
		assertThat(stored.getDescription()).isEqualTo("From the source");
		assertThat(stored.getContent()).isEqualTo("{\"id\":\"io.cibseven.mail\",\"name\":\"Mail 2\",\"description\":\"From the source\"}");
		assertThat(stored.getActive()).isFalse();
		assertThat(stored.getOrigin()).isEqualTo(ElementTemplateOrigin.DEFAULT_JSON);
	}

	@Test
	void keepsATemplateAUserHidOrEdited() throws Exception {
		ElementTemplate stored = stored(ElementTemplateOrigin.DEFAULT_JSON);
		stored.setActive(false);
		stored.setUpdatedBy("demo");

		load();

		verify(repository, never()).save(any());
		assertUntouched(stored);
		assertThat(stored.getActive()).isFalse();
		assertThat(stored.getUpdatedBy()).isEqualTo("demo");
	}

	// A copy keeps the origin of the template it was made from
	@Test
	void keepsATemplateAUserCreatedFromADefaultOne() throws Exception {
		ElementTemplate stored = stored(ElementTemplateOrigin.DEFAULT_JSON);
		stored.setCreatedBy("demo");

		load();

		verify(repository, never()).save(any());
		assertUntouched(stored);
		assertThat(stored.getCreatedBy()).isEqualTo("demo");
	}

	@ParameterizedTest
	@EnumSource(value = ElementTemplateOrigin.class, names = "DEFAULT_JSON", mode = EnumSource.Mode.EXCLUDE)
	void keepsATemplateOfTheSameIdThatDidNotComeFromTheSource(ElementTemplateOrigin origin) throws Exception {
		ElementTemplate stored = stored(origin);

		load();

		verify(repository, never()).save(any());
		assertUntouched(stored);
		assertThat(stored.getOrigin()).isEqualTo(origin);
	}

	private ElementTemplate stored(ElementTemplateOrigin origin) {
		ElementTemplate stored = new ElementTemplate();
		stored.setId("db-1");
		stored.setTemplateId("io.cibseven.mail");
		stored.setName("Mail");
		stored.setDescription("Changed by hand");
		stored.setContent("{\"id\":\"io.cibseven.mail\",\"name\":\"Mail\"}");
		stored.setOrigin(origin);
		when(repository.findElementTemplateById("io.cibseven.mail")).thenReturn(stored);
		return stored;
	}

	private static void assertUntouched(ElementTemplate stored) {
		assertThat(stored.getName()).isEqualTo("Mail");
		assertThat(stored.getDescription()).isEqualTo("Changed by hand");
		assertThat(stored.getContent()).isEqualTo("{\"id\":\"io.cibseven.mail\",\"name\":\"Mail\"}");
	}

	private void load() throws Exception {
		SevenWebclientProperties properties = new SevenWebclientProperties();
		properties.getModeler().getTemplates().setPaths(List.of("classpath:templates/*.json"));
		properties.getModeler().getTemplates().setSchemaReadyMaxAttempts(1);
		when(resources.getResources("classpath:templates/*.json"))
				.thenReturn(new Resource[] { new ByteArrayResource(SOURCE.getBytes(StandardCharsets.UTF_8), "mail.json") });
		new ElementTemplateLoader(repository, properties, mock(PlatformTransactionManager.class), resources,
				mock(WebclientExtensions.class)).load();
	}
}
