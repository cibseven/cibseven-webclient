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
package org.cibseven.modeler.rest;

import java.util.Map;
import java.util.Optional;

import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.model.FormEntity;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.provider.DBProcessDiagramProvider;
import org.cibseven.modeler.provider.FolderProvider;
import org.cibseven.modeler.provider.FormProvider;
import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.ModelerAccessChecker;
import org.cibseven.webapp.exception.ExistingFormIdException;
import org.cibseven.webapp.exception.ExistingProcessKeyException;
import org.cibseven.webapp.exception.InvalidFolderException;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Filing a model in another folder. A move writes only the folder; a copy is a new model, which
 * needs a key of its own because the engine resolves a process by its key and a form is referenced
 * by its form id.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModelerServiceMoveCopyTest {

	private static final String TARGET_FOLDER = "archive";

	@Mock private DBProcessDiagramProvider diagrams;
	@Mock private FormProvider forms;
	@Mock private FolderProvider folders;
	@Mock private ModelerAccessChecker modelerAccessChecker;
	@Mock @SuppressWarnings("rawtypes") private BaseUserProvider baseUserProvider;

	private ModelerService service;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() {
		service = new ModelerService();
		ReflectionTestUtils.setField(service, "dbProcessDiagramProvider", diagrams);
		ReflectionTestUtils.setField(service, "formProvider", forms);
		ReflectionTestUtils.setField(service, "folderProvider", folders);
		ReflectionTestUtils.setField(service, "modelerAccessChecker", modelerAccessChecker);
		ReflectionTestUtils.setField(service, "baseUserProvider", baseUserProvider);

		request = mock(HttpServletRequest.class);
		when(baseUserProvider.checkAuthorization(request, true)).thenReturn(new CIBUser("demo"));

		FolderEntity target = new FolderEntity();
		target.setId(TARGET_FOLDER);
		when(folders.requireModelFolder(TARGET_FOLDER)).thenReturn(target);
		when(diagrams.updateDiagram(any())).thenAnswer(call -> call.getArgument(0));
		when(diagrams.createDiagram(any())).thenAnswer(call -> call.getArgument(0));
		when(forms.updateForm(any())).thenAnswer(call -> call.getArgument(0));
		when(forms.createForm(any())).thenAnswer(call -> call.getArgument(0));
	}

	private ProcessDiagramEntity storedDiagram() {
		ProcessDiagramEntity stored = new ProcessDiagramEntity();
		stored.setId("diagram-1");
		stored.setName("Invoice");
		stored.setProcesskey("invoice");
		stored.setType("bpmn-c7");
		stored.setDiagram("<bpmn/>".getBytes());
		stored.setFolderId("general");
		when(diagrams.findById("diagram-1")).thenReturn(Optional.of(stored));
		return stored;
	}

	private FormEntity storedForm() {
		FormEntity stored = new FormEntity();
		stored.setId("form-1");
		stored.setFormId("invoice-form");
		stored.setFormSchema("{}".getBytes());
		stored.setFolderId("general");
		when(forms.findById("form-1")).thenReturn(Optional.of(stored));
		return stored;
	}

	@Test
	void movesADiagramWithoutChangingWhatIdentifiesIt() {
		storedDiagram();

		ProcessDiagramEntity moved = service.moveProcess("diagram-1", Map.of("folderId", TARGET_FOLDER), request);

		assertThat(moved.getFolderId()).isEqualTo(TARGET_FOLDER);
		assertThat(moved.getProcesskey()).isEqualTo("invoice");
		assertThat(moved.getUpdatedBy()).isEqualTo("demo");
	}

	@Test
	void movesAFormWithoutChangingWhatIdentifiesIt() {
		storedForm();

		FormEntity moved = service.moveForm("form-1", Map.of("folderId", TARGET_FOLDER), request);

		assertThat(moved.getFolderId()).isEqualTo(TARGET_FOLDER);
		assertThat(moved.getFormId()).isEqualTo("invoice-form");
	}

	@Test
	void reportsAMoveOfSomethingThatIsNotThere() {
		when(diagrams.findById("gone")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.moveProcess("gone", Map.of("folderId", TARGET_FOLDER), request))
			.isInstanceOf(NoObjectFoundException.class);
	}

	@Test
	void copiesADiagramIntoTheFolderWithItsOwnKey() {
		storedDiagram();

		ProcessDiagramEntity copy = service.copyProcess("diagram-1",
			Map.of("folderId", TARGET_FOLDER, "processkey", "invoice-copy"), request);

		assertThat(copy.getProcesskey()).isEqualTo("invoice-copy");
		assertThat(copy.getFolderId()).isEqualTo(TARGET_FOLDER);
		assertThat(copy.getDiagram()).isEqualTo("<bpmn/>".getBytes());
		// The name of the original, unless the request gives one
		assertThat(copy.getName()).isEqualTo("Invoice");
	}

	@Test
	void copiesADiagramUnderTheNameTheRequestGives() {
		storedDiagram();

		ProcessDiagramEntity copy = service.copyProcess("diagram-1",
			Map.of("folderId", TARGET_FOLDER, "processkey", "invoice-copy", "name", "Invoice (copy)"), request);

		assertThat(copy.getName()).isEqualTo("Invoice (copy)");
	}

	@Test
	void refusesADiagramCopyWithoutAKey() {
		storedDiagram();

		assertThatThrownBy(() -> service.copyProcess("diagram-1", Map.of("folderId", TARGET_FOLDER), request))
			.isInstanceOf(InvalidFolderException.class);
		verify(diagrams, never()).createDiagram(any());
	}

	@Test
	void refusesADiagramCopyWhoseKeyIsTaken() {
		storedDiagram();
		when(diagrams.findByProcessKey("invoice-copy")).thenReturn(new ProcessDiagramEntity());

		assertThatThrownBy(() -> service.copyProcess("diagram-1",
				Map.of("folderId", TARGET_FOLDER, "processkey", "invoice-copy"), request))
			.isInstanceOf(ExistingProcessKeyException.class);
		verify(diagrams, never()).createDiagram(any());
	}

	@Test
	void copiesAFormIntoTheFolderWithItsOwnFormId() {
		storedForm();

		FormEntity copy = service.copyForm("form-1",
			Map.of("folderId", TARGET_FOLDER, "formId", "invoice-form-copy"), request);

		assertThat(copy.getFormId()).isEqualTo("invoice-form-copy");
		assertThat(copy.getFolderId()).isEqualTo(TARGET_FOLDER);
		assertThat(copy.getFormSchema()).isEqualTo("{}".getBytes());
		assertThat(copy.getActive()).isTrue();
	}

	@Test
	void refusesAFormCopyWithoutAFormId() {
		storedForm();

		assertThatThrownBy(() -> service.copyForm("form-1", Map.of("folderId", TARGET_FOLDER), request))
			.isInstanceOf(InvalidFolderException.class);
		verify(forms, never()).createForm(any());
	}

	@Test
	void refusesAFormCopyWhoseFormIdIsTaken() {
		storedForm();
		when(forms.findByFormId("invoice-form-copy")).thenReturn(new FormEntity());

		assertThatThrownBy(() -> service.copyForm("form-1",
				Map.of("folderId", TARGET_FOLDER, "formId", "invoice-form-copy"), request))
			.isInstanceOf(ExistingFormIdException.class);
		verify(forms, never()).createForm(any());
	}

	/** Every one of these is behind the modeler permission, not only the reads. */
	@Test
	void refusesAllOfItWithoutModelerAccess() {
		org.mockito.Mockito.doThrow(new org.cibseven.webapp.exception.AccessDeniedException("no modeler"))
			.when(modelerAccessChecker).checkModelerAccess(any(CIBUser.class));

		Map<String, String> target = Map.of("folderId", TARGET_FOLDER, "processkey", "k", "formId", "f");
		assertThatThrownBy(() -> service.moveProcess("diagram-1", target, request)).isInstanceOf(org.cibseven.webapp.exception.AccessDeniedException.class);
		assertThatThrownBy(() -> service.copyProcess("diagram-1", target, request)).isInstanceOf(org.cibseven.webapp.exception.AccessDeniedException.class);
		assertThatThrownBy(() -> service.moveForm("form-1", target, request)).isInstanceOf(org.cibseven.webapp.exception.AccessDeniedException.class);
		assertThatThrownBy(() -> service.copyForm("form-1", target, request)).isInstanceOf(org.cibseven.webapp.exception.AccessDeniedException.class);
	}
}
