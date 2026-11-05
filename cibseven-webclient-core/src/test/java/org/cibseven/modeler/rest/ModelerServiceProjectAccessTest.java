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

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.model.FormEntity;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.provider.DBProcessDiagramProvider;
import org.cibseven.modeler.provider.FolderProvider;
import org.cibseven.modeler.provider.FormProvider;
import org.cibseven.modeler.provider.ProjectAccess.Action;
import org.cibseven.modeler.provider.ProjectGuard;
import org.cibseven.modeler.provider.UnifiedDiagramProvider;
import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.ModelerAccessChecker;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Every model endpoint asks the project its model lies in before it reads or changes anything. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModelerServiceProjectAccessTest {

	private static final CIBUser USER = new CIBUser("demo");

	@Mock private DBProcessDiagramProvider diagrams;
	@Mock private FormProvider forms;
	@Mock private UnifiedDiagramProvider unified;
	@Mock private FolderProvider folders;
	@Mock private ProjectGuard guard;
	@Mock private ModelerAccessChecker modelerAccessChecker;
	@Mock @SuppressWarnings("rawtypes") private BaseUserProvider baseUserProvider;

	private ModelerService service;
	private HttpServletRequest request;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		service = new ModelerService();
		ReflectionTestUtils.setField(service, "dbProcessDiagramProvider", diagrams);
		ReflectionTestUtils.setField(service, "formProvider", forms);
		ReflectionTestUtils.setField(service, "unifiedDiagramProvider", unified);
		ReflectionTestUtils.setField(service, "folderProvider", folders);
		ReflectionTestUtils.setField(service, "projectGuard", guard);
		ReflectionTestUtils.setField(service, "modelerAccessChecker", modelerAccessChecker);
		ReflectionTestUtils.setField(service, "baseUserProvider", baseUserProvider);

		request = mock(HttpServletRequest.class);
		when(baseUserProvider.checkAuthorization(request, true)).thenReturn(USER);
		when(folders.requireModelFolder(any())).thenAnswer(call -> {
			FolderEntity folder = new FolderEntity();
			folder.setId(call.getArgument(0));
			return folder;
		});
	}

	private ProcessDiagramEntity diagramIn(String folderId) {
		ProcessDiagramEntity diagram = new ProcessDiagramEntity();
		diagram.setId("diagram-1");
		diagram.setProcesskey("invoice");
		diagram.setType("bpmn-c7");
		diagram.setDiagram("<bpmn/>".getBytes());
		diagram.setFolderId(folderId);
		when(diagrams.findById("diagram-1")).thenReturn(Optional.of(diagram));
		when(diagrams.findByProcessKey(eq("invoice"), any())).thenReturn(List.of(diagram));
		return diagram;
	}

	private FormEntity formIn(String folderId) {
		FormEntity form = new FormEntity();
		form.setId("form-1");
		form.setFormId("invoice-form");
		form.setFolderId(folderId);
		when(forms.findById("form-1")).thenReturn(Optional.of(form));
		when(forms.findByFormId(eq("invoice-form"), any())).thenReturn(List.of(form));
		return form;
	}

	private MultiValueMap<String, MultipartFile> uploaded() throws Exception {
		MultipartFile file = mock(MultipartFile.class);
		when(file.getInputStream()).thenReturn(new ByteArrayInputStream("<bpmn/>".getBytes()));
		when(file.getBytes()).thenReturn("{}".getBytes());
		MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<>();
		files.add("diagram", file);
		return files;
	}

	private void refuse(String folderId, Action action) {
		doThrow(new AccessDeniedException("not allowed")).when(guard).require(USER, folderId, action);
	}

	@Test
	void readsADiagramOnlyForWhoMayViewItsProject() {
		diagramIn("claims");
		refuse("claims", Action.VIEW);

		assertThatThrownBy(() -> service.findById("diagram-1", request)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> service.findByIdData("diagram-1", request)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> service.findByKeyEntity("invoice", null, request)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> service.findByKey("invoice", null, request)).isInstanceOf(AccessDeniedException.class);
	}

	/** Nothing found stays a 404: there is no project to ask. */
	@Test
	void answersNotFoundWithoutAsking() {
		when(diagrams.findById("gone")).thenReturn(Optional.empty());

		assertThat(service.findById("gone", request).getStatusCode().value()).isEqualTo(404);
		verify(guard, never()).require(any(), any(), any());
	}

	@Test
	void deletesADiagramOrAFormOnlyForItsEditors() {
		diagramIn("claims");
		formIn("claims");
		refuse("claims", Action.EDIT);

		assertThatThrownBy(() -> service.delete("diagram-1", request)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> service.deleteForm("form-1", request)).isInstanceOf(AccessDeniedException.class);
		verify(diagrams, never()).delete(any());
		verify(forms, never()).delete(any());
	}

	/** The update carries no folder, so the stored diagram tells which project decides. */
	@Test
	void savesChangesOnlyForEditorsOfTheProjectTheDiagramIsIn() throws Exception {
		diagramIn("claims");
		refuse("claims", Action.EDIT);
		MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
		data.add("id", "diagram-1");
		MultiValueMap<String, MultipartFile> files = uploaded();

		assertThatThrownBy(() -> service.update(data, files, request))
			.isInstanceOf(AccessDeniedException.class);
		verify(diagrams, never()).updateDiagram(any());
	}

	@Test
	void createsAModelOnlyInAProjectTheUserEdits() throws Exception {
		refuse("claims", Action.EDIT);
		MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
		data.add("folderId", "claims");
		MultiValueMap<String, MultipartFile> files = uploaded();

		assertThatThrownBy(() -> service.save(data, files, request))
			.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> service.saveForm("invoice-form", files.getFirst("diagram"), "claims", request))
			.isInstanceOf(AccessDeniedException.class);
		verify(diagrams, never()).createDiagram(any());
		verify(forms, never()).createForm(any());
	}

	@Test
	void deploysAStoredDiagramOnlyForItsEditors() {
		diagramIn("claims");
		refuse("claims", Action.EDIT);

		assertThatThrownBy(() -> service.deployBpmnById("diagram-1", request)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void asksBothFoldersWhenAModelMoves() {
		diagramIn("claims");
		formIn("claims");

		service.moveProcess("diagram-1", Map.of("folderId", "drafts"), request);
		service.moveForm("form-1", Map.of("folderId", "drafts"), request);

		verify(guard, times(2)).requireMove(USER, "claims", "drafts");
	}

	/** A copy leaves the source as it is, so reading it is enough there. */
	@Test
	void copiesFromAProjectTheUserSeesIntoOneTheyEdit() {
		diagramIn("claims");
		formIn("claims");

		service.copyProcess("diagram-1", Map.of("folderId", "sales", "processkey", "invoice-copy"), request);
		service.copyForm("form-1", Map.of("folderId", "sales", "formId", "form-copy"), request);

		verify(guard, times(2)).require(USER, "claims", Action.VIEW);
		verify(guard, times(2)).require(USER, "sales", Action.EDIT);
	}

	@Test
	void leavesTheHiddenProjectsOutOfEveryList() {
		ProjectGuard.HiddenProjects hidden = new ProjectGuard.HiddenProjects(Set.of("claims"), folderId -> folderId);
		when(guard.hiddenProjects(USER)).thenReturn(hidden);

		service.getUnifiedDiagrams(request, 0, 20, null, null, null);
		service.getDiagrams(request, 0, 20, null, null);
		service.getForms(request, 0, 20, null);

		verify(unified).getDiagrams(null, null, null, hidden, 0, 20);
		verify(diagrams).getDiagrams(null, null, hidden, 0, 20);
		verify(forms).getForms(null, hidden, 0, 20);
	}

	@Test
	void listsAFolderOnlyForWhoMayViewIt() {
		refuse("claims", Action.VIEW);

		assertThatThrownBy(() -> service.getUnifiedDiagrams(request, 0, 20, null, null, "claims"))
			.isInstanceOf(AccessDeniedException.class);
		verify(unified, never()).getDiagrams(any(), any(), any(), any(), anyInt(),
			anyInt());
	}

	/** A key repeats across folders, so a lookup by it chooses among the visible models only (CIB7-2277). */
	@Test
	void looksUpAKeyAmongTheModelsTheUserMaySee() {
		ProjectGuard.HiddenProjects hidden = new ProjectGuard.HiddenProjects(Set.of("claims"), folderId -> folderId);
		when(guard.hiddenProjects(USER)).thenReturn(hidden);
		ProcessDiagramEntity visible = diagramIn("sales");
		when(diagrams.findByProcessKey("invoice", hidden)).thenReturn(List.of(visible));

		assertThat(service.findByKeyEntity("invoice", null, request).getBody()).isSameAs(visible);
	}

	@Test
	void looksUpAKeyInTheFolderNamedForWhoMayViewIt() {
		ProcessDiagramEntity diagram = diagramIn("sales");
		when(diagrams.findByProcessKeyInFolder("invoice", "sales")).thenReturn(diagram);
		FormEntity form = formIn("sales");
		when(forms.findByFormIdInFolder("invoice-form", "sales")).thenReturn(form);

		assertThat(service.findByKeyEntity("invoice", "sales", request).getBody()).isSameAs(diagram);
		assertThat(service.findByFormId("invoice-form", "sales", request).getBody()).isSameAs(form);
		verify(guard, times(4)).require(USER, "sales", Action.VIEW);

		refuse("claims", Action.VIEW);
		assertThatThrownBy(() -> service.findByKey("invoice", "claims", request)).isInstanceOf(AccessDeniedException.class);
		verify(diagrams, never()).findByProcessKeyInFolder("invoice", "claims");
	}

	@Test
	void listsTheFormsWithAFormIdTheUserMaySee() {
		ProjectGuard.HiddenProjects hidden = new ProjectGuard.HiddenProjects(Set.of("claims"), folderId -> folderId);
		when(guard.hiddenProjects(USER)).thenReturn(hidden);
		FormEntity form = formIn("sales");
		when(forms.findByFormId("invoice-form", hidden)).thenReturn(List.of(form));

		assertThat(service.findAllByFormId("invoice-form", request)).containsExactly(form);
	}

	/** It may be in a project hidden from the user, so the answer says only whether there is one. */
	@Test
	void tellsWhetherAnotherModelHasTheKey() {
		when(diagrams.isKeyUsedElsewhere("invoice", "diagram-1")).thenReturn(true);
		when(forms.isFormIdUsedElsewhere("invoice-form", null)).thenReturn(false);

		assertThat(service.isKeyInUse("invoice", "diagram-1", request)).isTrue();
		assertThat(service.isFormIdInUse("invoice-form", null, request)).isFalse();
	}

	@Test
	void deploysByKeyTheDiagramInTheFolderNamed() {
		ProcessDiagramEntity diagram = diagramIn("sales");
		when(diagrams.findByProcessKeyInFolder("invoice", "sales")).thenReturn(diagram);
		refuse("sales", Action.EDIT);

		assertThatThrownBy(() -> service.deployBpmnReduce("invoice", new LinkedMultiValueMap<>(), "sales", request))
			.isInstanceOf(AccessDeniedException.class);
		verify(diagrams).findByProcessKeyInFolder("invoice", "sales");
	}

	/** A later diagram the user may only look at does not stand in the way of one they may deploy. */
	@Test
	void deploysByKeyTheLatestDiagramTheUserMayEdit() throws Exception {
		ProcessDiagramEntity viewOnly = diagramIn("claims");
		ProcessDiagramEntity editable = new ProcessDiagramEntity();
		editable.setId("diagram-2");
		editable.setProcesskey("invoice");
		editable.setType("bpmn-c7");
		editable.setFolderId("sales");
		when(diagrams.findByProcessKey(eq("invoice"), any())).thenReturn(List.of(viewOnly, editable));
		refuse("claims", Action.EDIT);
		BpmProvider bpm = mock(BpmProvider.class);
		ReflectionTestUtils.setField(service, "bpmProvider", bpm);
		MultipartFile upload = mock(MultipartFile.class);
		when(upload.getBytes()).thenReturn("<bpmn/>".getBytes());
		MultiValueMap<String, MultipartFile> file = new LinkedMultiValueMap<>();
		file.add("file", upload);

		service.deployBpmnReduce("invoice", file, null, request);

		verify(guard, atLeastOnce()).require(USER, "sales", Action.EDIT);
		verify(bpm).deployBpmn(any(), any(), eq(USER));
	}

	/** A key is unique within a folder, so an import overwrites the model with its key there, not one of its name elsewhere. */
	@Test
	void overwritesOnImportTheDiagramWithTheKeyInTheFolderItGoesInto() throws Exception {
		ProcessDiagramEntity stored = diagramIn("sales");
		when(diagrams.findByProcessKeyInFolder("invoice", "sales")).thenReturn(stored);
		MultipartFile upload = mock(MultipartFile.class);
		when(upload.getOriginalFilename()).thenReturn("invoice.bpmn");
		when(upload.getBytes()).thenReturn(("<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">"
			+ "<bpmn:process id=\"invoice\" name=\"Invoice\"/></bpmn:definitions>").getBytes());
		MultiValueMap<String, MultipartFile> file = new LinkedMultiValueMap<>();
		file.add("file", upload);

		service.createProcessFromFile(file, true, "sales", request);

		verify(diagrams).updateDiagram(stored);
		verify(diagrams, never()).createDiagram(any());
		verify(diagrams, never()).findByName(any());
	}

	@Test
	void readsAFormOnlyForWhoMayViewItsProject() {
		formIn("claims");
		refuse("claims", Action.VIEW);

		assertThatThrownBy(() -> service.findFormById("form-1", request)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> service.findByFormId("invoice-form", null, request)).isInstanceOf(AccessDeniedException.class);
	}
}
