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
package org.cibseven.modeler.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.cibseven.modeler.model.ElementTemplate;
import org.cibseven.modeler.repository.ElementTemplateRepository;
import org.cibseven.webapp.exception.ExistingElementTemplateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Saving a template whose id is taken used to reach the database and fail on the unique index,
 * which surfaced the SQL statement as a 500 (CIB7 template management).
 */
class ElementTemplateProviderTest {

	private ElementTemplateRepository repository;
	private ElementTemplateProvider provider;

	@BeforeEach
	void setUp() {
		repository = Mockito.mock(ElementTemplateRepository.class);
		provider = new ElementTemplateProvider();
		ReflectionTestUtils.setField(provider, "repository", repository);
		when(repository.save(any(ElementTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private ElementTemplate template(String templateId) {
		ElementTemplate template = new ElementTemplate();
		template.setTemplateId(templateId);
		template.setName("One");
		return template;
	}

	@Test
	void rejectsATemplateIdThatIsAlreadyStored() {
		when(repository.findElementTemplateById("com.example.one")).thenReturn(template("com.example.one"));

		assertThatThrownBy(() -> provider.addTemplate(template("com.example.one")))
			.isInstanceOf(ExistingElementTemplateException.class)
			.satisfies(thrown -> assertThat(((ExistingElementTemplateException) thrown).getData())
				.containsExactly("com.example.one"));

		verify(repository, never()).save(any(ElementTemplate.class));
	}

	@Test
	void storesATemplateWithAFreeId() {
		when(repository.findElementTemplateById("com.example.two")).thenReturn(null);

		assertThat(provider.addTemplate(template("com.example.two")).getTemplateId()).isEqualTo("com.example.two");

		verify(repository).save(any(ElementTemplate.class));
	}
}
