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

import java.util.Optional;

import org.cibseven.modeler.model.FormEntity;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.repository.FormRepository;
import org.cibseven.modeler.repository.ProcessDiagramRepository;
import org.cibseven.webapp.exception.ValueTooLongException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A value longer than its column. Left to the database it comes back as a 500 carrying the failed
 * INSERT, which tells the user nothing about what to shorten and shows them the schema; these are
 * refused as request errors naming the field instead.
 */
class ModelLengthTest {

	private ProcessDiagramRepository diagrams;
	private FormRepository forms;
	private DBProcessDiagramProvider diagramProvider;
	private FormProvider formProvider;

	@BeforeEach
	void setUp() {
		diagrams = Mockito.mock(ProcessDiagramRepository.class);
		forms = Mockito.mock(FormRepository.class);
		diagramProvider = new DBProcessDiagramProvider();
		formProvider = new FormProvider();
		ReflectionTestUtils.setField(diagramProvider, "processDiagramDao", diagrams);
		ReflectionTestUtils.setField(formProvider, "formRepositoryDao", forms);
		when(diagrams.save(any())).thenAnswer(call -> call.getArgument(0));
		when(forms.save(any())).thenAnswer(call -> call.getArgument(0));
	}

	private static String of(int length) {
		return "x".repeat(length);
	}

	private ProcessDiagramEntity diagram(String name, String key) {
		ProcessDiagramEntity entity = new ProcessDiagramEntity();
		entity.setId("diagram-1");
		entity.setName(name);
		entity.setProcesskey(key);
		return entity;
	}

	@Test
	void refusesAProcessKeyLongerThanItsColumn() {
		assertThatThrownBy(() -> diagramProvider.createDiagram(diagram("Invoice", of(101))))
			.isInstanceOf(ValueTooLongException.class)
			.satisfies(thrown -> {
				assertThat(((ValueTooLongException) thrown).getField()).isEqualTo("processkey");
				assertThat(((ValueTooLongException) thrown).getLimit()).isEqualTo(100);
			});
		verify(diagrams, never()).save(any());
	}

	@Test
	void refusesANameLongerThanItsColumn() {
		assertThatThrownBy(() -> diagramProvider.createDiagram(diagram(of(256), "invoice")))
			.isInstanceOf(ValueTooLongException.class)
			.satisfies(thrown -> assertThat(((ValueTooLongException) thrown).getField()).isEqualTo("name"));
	}

	@Test
	void refusesADescriptionLongerThanItsColumn() {
		ProcessDiagramEntity entity = diagram("Invoice", "invoice");
		entity.setDescription(of(151));

		assertThatThrownBy(() -> diagramProvider.createDiagram(entity))
			.isInstanceOf(ValueTooLongException.class)
			.satisfies(thrown -> assertThat(((ValueTooLongException) thrown).getField()).isEqualTo("description"));
	}

	/** An update reaches the same columns, so it is checked before it is written. */
	@Test
	void refusesAnUpdateThatWouldNotFit() {
		when(diagrams.findById("diagram-1")).thenReturn(Optional.of(diagram("Invoice", "invoice")));

		assertThatThrownBy(() -> diagramProvider.updateDiagram(diagram("Invoice", of(101))))
			.isInstanceOf(ValueTooLongException.class);
		verify(diagrams, never()).save(any());
	}

	@Test
	void acceptsValuesThatFillTheColumnExactly() {
		ProcessDiagramEntity entity = diagram(of(255), of(100));
		entity.setDescription(of(150));

		assertThat(diagramProvider.createDiagram(entity).getProcesskey()).hasSize(100);
	}

	@Test
	void refusesAFormIdLongerThanItsColumn() {
		FormEntity entity = new FormEntity();
		entity.setFormId(of(101));

		assertThatThrownBy(() -> formProvider.createForm(entity))
			.isInstanceOf(ValueTooLongException.class)
			.satisfies(thrown -> assertThat(((ValueTooLongException) thrown).getField()).isEqualTo("formId"));
		verify(forms, never()).save(any());
	}

	@Test
	void acceptsAFormIdThatFillsTheColumnExactly() {
		FormEntity entity = new FormEntity();
		entity.setFormId(of(100));

		assertThat(formProvider.createForm(entity).getFormId()).hasSize(100);
	}
}
