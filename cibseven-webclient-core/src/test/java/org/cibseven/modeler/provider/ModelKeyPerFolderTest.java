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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.cibseven.modeler.model.FormEntity;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.repository.FormRepository;
import org.cibseven.modeler.repository.ProcessDiagramRepository;
import org.cibseven.webapp.exception.ExistingFormIdException;
import org.cibseven.webapp.exception.ExistingProcessKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A process key and a form id are unique within a folder, not across folders (CIB7-2277): two
 * files of the same process may sit side by side, as they would on disk.
 */
class ModelKeyPerFolderTest {

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

	private static ProcessDiagramEntity diagram(String id, String key, String folderId) {
		ProcessDiagramEntity diagram = new ProcessDiagramEntity();
		diagram.setId(id);
		diagram.setProcesskey(key);
		diagram.setFolderId(folderId);
		return diagram;
	}

	private static FormEntity form(String id, String formId, String folderId) {
		FormEntity form = new FormEntity();
		form.setId(id);
		form.setFormId(formId);
		form.setFolderId(folderId);
		return form;
	}

	@Test
	void storesADiagramWhoseKeyOnlyAnotherFolderHas() {
		ProcessDiagramEntity created = diagramProvider.createDiagram(diagram(null, "invoice", "sales"));

		assertThat(created.getFolderId()).isEqualTo("sales");
		verify(diagrams).findByProcesskeyAndFolderId("invoice", "sales");
	}

	@Test
	void refusesADiagramWhoseKeyTheFolderHas() {
		when(diagrams.findByProcesskeyAndFolderId("invoice", "sales")).thenReturn(diagram("d-1", "invoice", "sales"));

		assertThatThrownBy(() -> diagramProvider.createDiagram(diagram(null, "invoice", "sales")))
			.isInstanceOf(ExistingProcessKeyException.class);
		verify(diagrams, never()).save(any());
	}

	/** Saving a diagram again finds it holding its own key, which is no conflict. */
	@Test
	void updatesADiagramThatHoldsTheKeyItself() {
		ProcessDiagramEntity stored = diagram("d-1", "invoice", "sales");
		when(diagrams.findById("d-1")).thenReturn(Optional.of(stored));
		when(diagrams.findByProcesskeyAndFolderId("invoice", "sales")).thenReturn(stored);

		assertThatCode(() -> diagramProvider.updateDiagram(diagram("d-1", "invoice", null))).doesNotThrowAnyException();
	}

	@Test
	void refusesToRenameADiagramToAKeyTheFolderHas() {
		when(diagrams.findById("d-1")).thenReturn(Optional.of(diagram("d-1", "invoice", "sales")));
		when(diagrams.findByProcesskeyAndFolderId("order", "sales")).thenReturn(diagram("d-2", "order", "sales"));

		assertThatThrownBy(() -> diagramProvider.updateDiagram(diagram("d-1", "order", null)))
			.isInstanceOf(ExistingProcessKeyException.class);
	}

	/** A move goes through the update, so the folder it goes to is the one checked. */
	@Test
	void refusesToMoveADiagramIntoAFolderThatHasItsKey() {
		when(diagrams.findById("d-1")).thenReturn(Optional.of(diagram("d-1", "invoice", "sales")));
		when(diagrams.findByProcesskeyAndFolderId("invoice", "archive")).thenReturn(diagram("d-2", "invoice", "archive"));

		assertThatThrownBy(() -> diagramProvider.updateDiagram(diagram("d-1", "invoice", "archive")))
			.isInstanceOf(ExistingProcessKeyException.class);
	}

	@Test
	void findsTheLatestDiagramWithAKeyInAnyFolder() {
		ProcessDiagramEntity latest = diagram("d-2", "invoice", "archive");
		when(diagrams.findByProcesskeyOrderByUpdatedDesc("invoice"))
			.thenReturn(List.of(latest, diagram("d-1", "invoice", "sales")));

		assertThat(diagramProvider.findByProcessKey("invoice")).isSameAs(latest);
		assertThat(diagramProvider.findByProcessKey("nope")).isNull();
	}

	/** A lookup by key never picks a diagram in a project the user may not see. */
	@Test
	void leavesTheDiagramsOfHiddenProjectsOutOfALookupByKey() {
		ProcessDiagramEntity visible = diagram("d-1", "invoice", "sales");
		when(diagrams.findByProcesskeyOrderByUpdatedDesc("invoice"))
			.thenReturn(List.of(diagram("d-2", "invoice", "claims"), visible));
		ProjectGuard.HiddenProjects hidden = new ProjectGuard.HiddenProjects(Set.of("claims"), folderId -> folderId);

		assertThat(diagramProvider.findByProcessKey("invoice", hidden)).containsExactly(visible);
	}

	@Test
	void tellsWhetherAnotherDiagramHasTheKey() {
		when(diagrams.findByProcesskeyOrderByUpdatedDesc("invoice")).thenReturn(List.of(diagram("d-1", "invoice", "sales")));

		assertThat(diagramProvider.isKeyUsedElsewhere("invoice", "d-1")).isFalse();
		assertThat(diagramProvider.isKeyUsedElsewhere("invoice", "d-2")).isTrue();
		// A diagram not saved yet is no diagram the key could belong to
		assertThat(diagramProvider.isKeyUsedElsewhere("invoice", null)).isTrue();
		assertThat(diagramProvider.isKeyUsedElsewhere("order", null)).isFalse();
	}

	@Test
	void refusesAFormWhoseFormIdTheFolderHas() {
		when(forms.findByFormIdAndFolderId("invoice-form", "sales")).thenReturn(form("f-1", "invoice-form", "sales"));

		assertThatThrownBy(() -> formProvider.createForm(form(null, "invoice-form", "sales")))
			.isInstanceOf(ExistingFormIdException.class);
		assertThatCode(() -> formProvider.createForm(form(null, "invoice-form", "archive"))).doesNotThrowAnyException();
	}

	@Test
	void refusesToMoveAFormIntoAFolderThatHasItsFormId() {
		FormEntity stored = form("f-1", "invoice-form", "sales");
		when(forms.findById("f-1")).thenReturn(Optional.of(stored));
		when(forms.findByFormIdAndFolderId("invoice-form", "sales")).thenReturn(stored);
		when(forms.findByFormIdAndFolderId("invoice-form", "archive")).thenReturn(form("f-2", "invoice-form", "archive"));

		assertThatThrownBy(() -> formProvider.updateForm(form("f-1", null, "archive")))
			.isInstanceOf(ExistingFormIdException.class);
		// Saving it where it is finds only itself
		assertThatCode(() -> formProvider.updateForm(form("f-1", null, null))).doesNotThrowAnyException();
	}

	@Test
	void findsTheFormsWithAFormIdTheUserMaySee() {
		FormEntity latest = form("f-2", "invoice-form", "archive");
		FormEntity hiddenForm = form("f-3", "invoice-form", "claims");
		when(forms.findByFormIdOrderByUpdatedDesc("invoice-form"))
			.thenReturn(List.of(latest, hiddenForm, form("f-1", "invoice-form", "sales")));
		ProjectGuard.HiddenProjects hidden = new ProjectGuard.HiddenProjects(Set.of("claims"), folderId -> folderId);

		assertThat(formProvider.findByFormId("invoice-form")).isSameAs(latest);
		assertThat(formProvider.findByFormId("invoice-form", hidden)).extracting(FormEntity::getId).containsExactly("f-2", "f-1");
		assertThat(formProvider.isFormIdUsedElsewhere("invoice-form", "f-1")).isTrue();
		assertThat(formProvider.findByFormIdInFolder("invoice-form", "sales")).isNull();
	}
}
