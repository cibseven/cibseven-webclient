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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Reads the element template files with the webclient's own JSON mapper (CIB7-1535): unknown
 * properties are ignored, the template's {@code id} is its template id, and a broken file is skipped
 * instead of ending the load.
 */
class ElementTemplateLoaderReadingTest {

	private final ElementTemplateRepository repository = mock(ElementTemplateRepository.class);
	private final ResourcePatternResolver resources = mock(ResourcePatternResolver.class);
	private final WebclientExtensions extensions = mock(WebclientExtensions.class);

	@Test
	void loadsTheTemplatesOfEveryReadableFile() throws Exception {
		SevenWebclientProperties properties = new SevenWebclientProperties();
		properties.getModeler().getTemplates().setPaths(List.of("classpath:templates/*.json"));
		properties.getModeler().getTemplates().setSchemaReadyMaxAttempts(1);
		when(resources.getResources("classpath:templates/*.json")).thenReturn(new Resource[] {
				resource("broken.json", "[{\"id\":\"io.cibseven.broken\","),
				resource("mail.json", "[{\"$schema\":\"https://unpkg.com/@camunda/element-templates-json-schema/resources/schema.json\","
						+ "\"id\":\"io.cibseven.mail\",\"name\":\"Mail\",\"version\":3,\"appliesTo\":[\"bpmn:ServiceTask\"],"
						+ "\"properties\":[{\"label\":\"To\",\"type\":\"String\",\"binding\":{\"type\":\"camunda:inputParameter\",\"name\":\"to\"}}]},"
						+ "{\"id\":\"io.cibseven.sms\",\"name\":\"SMS\",\"description\":\"Größe 😀\"}]"),
				resource("single.json", "{\"id\":\"io.cibseven.single\",\"name\":\"Single\",\"origin\":\"USER\",\"active\":false}")
		});
		ElementTemplateLoader loader = new ElementTemplateLoader(repository, properties,
				mock(PlatformTransactionManager.class), resources, extensions);

		loader.load();

		ArgumentCaptor<ElementTemplate> saved = ArgumentCaptor.forClass(ElementTemplate.class);
		verify(repository, times(3)).save(saved.capture());
		assertThat(saved.getAllValues()).extracting(ElementTemplate::getTemplateId)
				.containsExactly("io.cibseven.mail", "io.cibseven.sms", "io.cibseven.single");
		assertThat(saved.getAllValues()).allSatisfy(template -> {
			assertThat(template.getId()).isNull();
			assertThat(template.getOrigin()).isEqualTo(ElementTemplateOrigin.DEFAULT_JSON);
			// read-only for uploads and for the files
			assertThat(template.getActive()).isTrue();
			assertThat(template.getVersion()).isEqualTo(1);
		});
		// the template is stored as it was declared
		assertThat(saved.getAllValues().get(0).getContent()).startsWith("{\"$schema\":").contains("\"binding\":{\"type\":\"camunda:inputParameter\"");
		assertThat(saved.getAllValues().get(1).getContent()).isEqualTo("{\"id\":\"io.cibseven.sms\",\"name\":\"SMS\",\"description\":\"Größe 😀\"}");
		assertThat(saved.getAllValues().get(1).getDescription()).isEqualTo("Größe 😀");
	}

	private static Resource resource(String name, String json) {
		return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8), name);
	}
}
