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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * An update copies named fields onto the row that is already stored, so a field it does not name
 * is discarded without a word. The folder was such a field: a move reported success, returned the
 * old folder and left the model where it was.
 */
class ModelUpdateFolderTest {

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

	private ProcessDiagramEntity storedDiagram() {
		ProcessDiagramEntity stored = new ProcessDiagramEntity();
		stored.setId("diagram-1");
		stored.setFolderId("general");
		when(diagrams.findById("diagram-1")).thenReturn(Optional.of(stored));
		return stored;
	}

	private FormEntity storedForm() {
		FormEntity stored = new FormEntity();
		stored.setId("form-1");
		stored.setFolderId("general");
		when(forms.findById("form-1")).thenReturn(Optional.of(stored));
		return stored;
	}

	@Test
	void movesADiagramToTheFolderTheUpdateNames() {
		storedDiagram();
		ProcessDiagramEntity move = new ProcessDiagramEntity();
		move.setId("diagram-1");
		move.setFolderId("archive");

		assertThat(diagramProvider.updateDiagram(move).getFolderId()).isEqualTo("archive");
	}

	/** Replacing the content of a diagram on import says nothing about where it lives. */
	@Test
	void leavesADiagramWhereItIsWhenTheUpdateNamesNoFolder() {
		storedDiagram();
		ProcessDiagramEntity update = new ProcessDiagramEntity();
		update.setId("diagram-1");
		update.setDiagram("<bpmn/>".getBytes());

		assertThat(diagramProvider.updateDiagram(update).getFolderId()).isEqualTo("general");
	}

	@Test
	void movesAFormToTheFolderTheUpdateNames() {
		storedForm();
		FormEntity move = new FormEntity();
		move.setId("form-1");
		move.setFolderId("archive");

		assertThat(formProvider.updateForm(move).getFolderId()).isEqualTo("archive");
	}

	@Test
	void leavesAFormWhereItIsWhenTheUpdateNamesNoFolder() {
		storedForm();
		FormEntity update = new FormEntity();
		update.setId("form-1");
		update.setFormSchema("{}".getBytes());

		assertThat(formProvider.updateForm(update).getFolderId()).isEqualTo("general");
	}
}
