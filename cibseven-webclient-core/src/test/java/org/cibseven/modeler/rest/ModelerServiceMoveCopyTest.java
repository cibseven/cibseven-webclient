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

import java.nio.charset.StandardCharsets;
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
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

	/** A key is unique only within a folder, so a copy elsewhere may keep it (CIB7-2277). */
	@Test
	void keepsTheKeyOfADiagramCopiedWithoutOne() {
		storedDiagram();

		ProcessDiagramEntity copy = service.copyProcess("diagram-1", Map.of("folderId", TARGET_FOLDER), request);

		assertThat(copy.getProcesskey()).isEqualTo("invoice");
		assertThat(copy.getFolderId()).isEqualTo(TARGET_FOLDER);
	}

	/**
	 * The modeler saves the key it reads from the model, and the engine deploys it: a new key only
	 * on the stored copy would clash with the original on the next save (CIB7-2679).
	 */
	@Test
	void writesTheNewKeyIntoTheCopiedModel() {
		ProcessDiagramEntity stored = storedDiagram();
		stored.setDiagram(processXml("invoice"));

		ProcessDiagramEntity copy = service.copyProcess("diagram-1",
			Map.of("folderId", TARGET_FOLDER, "processkey", "invoice-copy"), request);

		assertThat(new String(copy.getDiagram(), StandardCharsets.UTF_8)).isEqualTo(new String(processXml("invoice-copy"), StandardCharsets.UTF_8));
		// The original keeps its own
		assertThat(stored.getDiagram()).isEqualTo(processXml("invoice"));
	}

	@Test
	void leavesTheModelOfACopyThatKeepsItsKeyAsItIs() {
		ProcessDiagramEntity stored = storedDiagram();
		stored.setDiagram(processXml("invoice"));

		ProcessDiagramEntity copy = service.copyProcess("diagram-1", Map.of("folderId", TARGET_FOLDER), request);

		assertThat(copy.getDiagram()).isEqualTo(processXml("invoice"));
	}

	/**
	 * A model imported through the backend may be stored under another id than the one the modeler
	 * reads; the copy carries its new key where the modeler reads it all the same.
	 */
	@Test
	void writesTheNewKeyWhereTheModelerReadsItWhateverKeyIsStored() {
		ProcessDiagramEntity stored = storedDiagram();
		stored.setDiagram(processXml("Process_1"));

		ProcessDiagramEntity copy = service.copyProcess("diagram-1",
			Map.of("folderId", TARGET_FOLDER, "processkey", "invoice-copy"), request);

		assertThat(copy.getDiagram()).isEqualTo(processXml("invoice-copy"));
	}

	private static byte[] processXml(String key) {
		return ("<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
			+ "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" id=\"d\"><bpmn:process id=\"" + key
			+ "\" name=\"Invoice\" /><bpmndi:BPMNDiagram id=\"D\"><bpmndi:BPMNPlane id=\"p\" bpmnElement=\"" + key
			+ "\" /></bpmndi:BPMNDiagram></bpmn:definitions>").getBytes(StandardCharsets.UTF_8);
	}

	/** The provider refuses a key the folder has already, and the copy is not stored. */
	@Test
	void passesOnTheRefusalOfAKeyTheFolderHas() {
		storedDiagram();
		when(diagrams.createDiagram(any())).thenThrow(new ExistingProcessKeyException("invoice"));

		assertThatThrownBy(() -> service.copyProcess("diagram-1", Map.of("folderId", TARGET_FOLDER), request))
			.isInstanceOf(ExistingProcessKeyException.class);
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
	void keepsTheFormIdOfAFormCopiedWithoutOne() {
		storedForm();

		FormEntity copy = service.copyForm("form-1", Map.of("folderId", TARGET_FOLDER), request);

		assertThat(copy.getFormId()).isEqualTo("invoice-form");
		assertThat(copy.getFolderId()).isEqualTo(TARGET_FOLDER);
	}

	/** The form editor saves the id it reads from the schema (CIB7-2679). */
	@Test
	void writesTheNewFormIdIntoTheCopiedSchema() throws Exception {
		FormEntity stored = storedForm();
		byte[] schema = "{\"type\":\"default\",\"id\":\"invoice-form\",\"components\":[]}".getBytes(StandardCharsets.UTF_8);
		stored.setFormSchema(schema);

		FormEntity copy = service.copyForm("form-1",
			Map.of("folderId", TARGET_FOLDER, "formId", "invoice-form-copy"), request);

		assertThat(new ObjectMapper().readTree(copy.getFormSchema()).get("id").asText()).isEqualTo("invoice-form-copy");
		assertThat(stored.getFormSchema()).isEqualTo(schema);
	}

	@Test
	void passesOnTheRefusalOfAFormIdTheFolderHas() {
		storedForm();
		when(forms.createForm(any())).thenThrow(new ExistingFormIdException("invoice-form"));

		assertThatThrownBy(() -> service.copyForm("form-1", Map.of("folderId", TARGET_FOLDER), request))
			.isInstanceOf(ExistingFormIdException.class);
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
