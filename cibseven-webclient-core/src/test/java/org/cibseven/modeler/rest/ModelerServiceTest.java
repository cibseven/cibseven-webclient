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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.cibseven.modeler.model.FormEntity;
import org.cibseven.modeler.model.FormUsageEntity;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.model.UnifiedDiagram;
import org.cibseven.modeler.provider.DBProcessDiagramProvider;
import org.cibseven.modeler.provider.DiagramUsageProvider;
import org.cibseven.modeler.provider.FormProvider;
import org.cibseven.modeler.provider.FormUsageProvider;
import org.cibseven.modeler.provider.UnifiedDiagramProvider;
import org.cibseven.modeler.provider.UserSessionProvider;
import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.ModelerAccessChecker;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.providers.BpmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The modeler's CRUD surface over diagrams and forms. Every endpoint is gated by
 * {@link ModelerAccessChecker}, and the read endpoints have to answer 404 rather than an empty
 * 200 when the entity is gone.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ModelerServiceTest {

	@Mock private DBProcessDiagramProvider dbProcessDiagramProvider;
	@Mock private DiagramUsageProvider diagramUsageProvider;
	@Mock private FormUsageProvider formUsageProvider;
	@Mock private UserSessionProvider userSessionProvider;
	@Mock private FormProvider formProvider;
	@Mock private UnifiedDiagramProvider unifiedDiagramProvider;
	@Mock private ModelerAccessChecker modelerAccessChecker;
	@Mock private BpmProvider bpmProvider;
	@Mock @SuppressWarnings("rawtypes") private BaseUserProvider baseUserProvider;

	private ModelerService service;
	private HttpServletRequest request;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		service = new ModelerService();
		ReflectionTestUtils.setField(service, "dbProcessDiagramProvider", dbProcessDiagramProvider);
		ReflectionTestUtils.setField(service, "diagramUsageProvider", diagramUsageProvider);
		ReflectionTestUtils.setField(service, "formUsageProvider", formUsageProvider);
		ReflectionTestUtils.setField(service, "userSessionProvider", userSessionProvider);
		ReflectionTestUtils.setField(service, "formProvider", formProvider);
		ReflectionTestUtils.setField(service, "unifiedDiagramProvider", unifiedDiagramProvider);
		ReflectionTestUtils.setField(service, "modelerAccessChecker", modelerAccessChecker);
		ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(service, "baseUserProvider", baseUserProvider);

		request = mock(HttpServletRequest.class);
		user = new CIBUser("demo");
		when(baseUserProvider.checkAuthorization(request, true)).thenReturn(user);
	}

	private ProcessDiagramEntity diagram(String id, String name, String key) {
		ProcessDiagramEntity entity = new ProcessDiagramEntity();
		entity.setId(id);
		entity.setName(name);
		entity.setProcesskey(key);
		entity.setDiagram("<definitions/>".getBytes(StandardCharsets.UTF_8));
		return entity;
	}

	private FormEntity form(String id, String formId) {
		FormEntity entity = new FormEntity();
		entity.setId(id);
		entity.setFormId(formId);
		entity.setFormSchema("{}".getBytes(StandardCharsets.UTF_8));
		return entity;
	}

	// ---------- access control ----------

	@Test
	void everyEndpointRequiresModelerAccess() {
		doThrow(new AccessDeniedException("no modeler")).when(modelerAccessChecker).checkModelerAccess(user);

		assertThatThrownBy(() -> service.getDiagrams(request, 0, 10, null, null))
			.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> service.findById("d-1", request))
			.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> service.delete("d-1", request))
			.isInstanceOf(AccessDeniedException.class);
		verify(dbProcessDiagramProvider, never()).delete("d-1");
	}

	// ---------- diagram listing ----------

	@Test
	void getDiagrams_forwardsThePagingAndFilters() {
		when(dbProcessDiagramProvider.getDiagrams("invoice", "bpmn-c7", 0, 10)).thenReturn(List.of());

		service.getDiagrams(request, 0, 10, "bpmn-c7", "invoice");

		verify(dbProcessDiagramProvider).getDiagrams("invoice", "bpmn-c7", 0, 10);
	}

	@Test
	void getUnifiedDiagrams_forwardsThePagingAndFilters() {
		when(unifiedDiagramProvider.getDiagrams("invoice", "bpmn", 5, 20)).thenReturn(List.of());

		service.getUnifiedDiagrams(request, 5, 20, "invoice", "bpmn");

		verify(unifiedDiagramProvider).getDiagrams("invoice", "bpmn", 5, 20);
	}

	@Test
	void getUnifiedDiagramById_answers404WhenItIsGone() {
		when(unifiedDiagramProvider.getDiagramById("missing")).thenReturn(Optional.empty());

		assertThat(service.getUnifiedDiagramById("missing", request).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void getUnifiedDiagramById_returnsTheDiagram() {
		UnifiedDiagram unified = mock(UnifiedDiagram.class);
		when(unifiedDiagramProvider.getDiagramById("d-1")).thenReturn(Optional.of(unified));

		ResponseEntity<UnifiedDiagram> response = service.getUnifiedDiagramById("d-1", request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isSameAs(unified);
	}

	// ---------- diagram lookup ----------

	@Test
	void findById_returnsTheEntity() {
		when(dbProcessDiagramProvider.findById("d-1")).thenReturn(Optional.of(diagram("d-1", "Invoice", "invoice")));

		ResponseEntity<ProcessDiagramEntity> response = service.findById("d-1", request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getName()).isEqualTo("Invoice");
	}

	@Test
	void findById_answers404WhenItIsGone() {
		when(dbProcessDiagramProvider.findById("missing")).thenReturn(Optional.empty());

		assertThat(service.findById("missing", request).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void findByIdData_servesTheDiagramAsAnOctetStream() {
		when(dbProcessDiagramProvider.findById("d-1")).thenReturn(Optional.of(diagram("d-1", "Invoice", "invoice")));

		ResponseEntity<byte[]> response = service.findByIdData("d-1", request);

		assertThat(response.getBody()).isEqualTo("<definitions/>".getBytes(StandardCharsets.UTF_8));
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Test
	void findByIdData_answers404WhenItIsGone() {
		when(dbProcessDiagramProvider.findById("missing")).thenReturn(Optional.empty());

		assertThat(service.findByIdData("missing", request).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void findByName_servesTheDiagram() {
		when(dbProcessDiagramProvider.findByName("Invoice")).thenReturn(diagram("d-1", "Invoice", "invoice"));

		ResponseEntity<byte[]> response = service.findByName("Invoice", request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotEmpty();
	}

	@Test
	void findByName_answers404WhenThereIsNoSuchName() {
		when(dbProcessDiagramProvider.findByName("Nope")).thenReturn(null);

		assertThat(service.findByName("Nope", request).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void findByKey_servesTheDiagram() {
		when(dbProcessDiagramProvider.findByProcessKey("invoice")).thenReturn(diagram("d-1", "Invoice", "invoice"));

		assertThat(service.findByKey("invoice", request).getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void findByKey_answers404WhenThereIsNoSuchKey() {
		when(dbProcessDiagramProvider.findByProcessKey("nope")).thenReturn(null);

		assertThat(service.findByKey("nope", request).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void findByKeyEntity_returnsTheEntity() {
		when(dbProcessDiagramProvider.findByProcessKey("invoice")).thenReturn(diagram("d-1", "Invoice", "invoice"));

		assertThat(service.findByKeyEntity("invoice", request).getBody().getId()).isEqualTo("d-1");
	}

	@Test
	void findByKeyEntity_answers404WhenThereIsNoSuchKey() {
		when(dbProcessDiagramProvider.findByProcessKey("nope")).thenReturn(null);

		assertThat(service.findByKeyEntity("nope", request).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void delete_removesTheDiagram() {
		service.delete("d-1", request);

		verify(dbProcessDiagramProvider).delete("d-1");
	}

	// ---------- forms ----------

	@Test
	void getForms_forwardsThePagingAndKeyword() {
		when(formProvider.getForms("invoice", 0, 10)).thenReturn(List.of());

		service.getForms(request, 0, 10, "invoice");

		verify(formProvider).getForms("invoice", 0, 10);
	}

	@Test
	void saveForm_storesTheSchemaAndStampsTheAuthor() {
		MockMultipartFile schema = new MockMultipartFile("form_schema", "f.json",
			"application/json", "{\"components\":[]}".getBytes(StandardCharsets.UTF_8));
		when(formProvider.createForm(any())).thenAnswer(invocation -> invocation.getArgument(0));

		FormEntity created = service.saveForm("invoice-form", schema, request);

		assertThat(created.getFormId()).isEqualTo("invoice-form");
		assertThat(new String(created.getFormSchema(), StandardCharsets.UTF_8)).contains("components");
		assertThat(created.getUpdatedBy()).isEqualTo("demo");
	}

	@Test
	void saveForm_storesANullSchemaWhenTheUploadCannotBeRead() throws Exception {
		MultipartFile unreadable = mock(MultipartFile.class);
		when(unreadable.getBytes()).thenThrow(new IOException("stream closed"));
		when(formProvider.createForm(any())).thenAnswer(invocation -> invocation.getArgument(0));

		FormEntity created = service.saveForm("invoice-form", unreadable, request);

		// the form is still created, just without a schema
		assertThat(created.getFormSchema()).isNull();
	}

	@Test
	void updateForm_savesTheNewSchema() {
		MockMultipartFile schema = new MockMultipartFile("form_schema", "f.json",
			"application/json", "{}".getBytes(StandardCharsets.UTF_8));
		when(formProvider.updateForm(any())).thenAnswer(invocation -> invocation.getArgument(0));

		FormEntity updated = service.updateForm("f-1", "invoice-form", schema, request);

		assertThat(updated.getId()).isEqualTo("f-1");
		assertThat(updated.getUpdatedBy()).isEqualTo("demo");
	}

	@Test
	void updateForm_renewsTheEditingSessionOfTheSameUser() {
		MockMultipartFile schema = new MockMultipartFile("form_schema", "f.json", "application/json", "{}".getBytes());
		when(formProvider.updateForm(any())).thenAnswer(invocation -> invocation.getArgument(0));
		FormUsageEntity usage = new FormUsageEntity();
		usage.setUserId("demo");
		when(formUsageProvider.checkSessionUser("f-1")).thenReturn(usage);

		service.updateForm("f-1", "invoice-form", schema, request);

		// saving keeps the editing lock alive for its owner
		verify(formUsageProvider).createFormUsage(usage);
	}

	@Test
	void updateForm_doesNotRenewAnotherUsersEditingSession() {
		MockMultipartFile schema = new MockMultipartFile("form_schema", "f.json", "application/json", "{}".getBytes());
		when(formProvider.updateForm(any())).thenAnswer(invocation -> invocation.getArgument(0));
		FormUsageEntity usage = new FormUsageEntity();
		usage.setUserId("someone-else");
		when(formUsageProvider.checkSessionUser("f-1")).thenReturn(usage);

		service.updateForm("f-1", "invoice-form", schema, request);

		verify(formUsageProvider, never()).createFormUsage(any());
	}

	@Test
	void updateForm_doesNothingExtraWhenTheUpdateDidNotApply() {
		MockMultipartFile schema = new MockMultipartFile("form_schema", "f.json", "application/json", "{}".getBytes());
		when(formProvider.updateForm(any())).thenReturn(null);

		assertThat(service.updateForm("f-1", "invoice-form", schema, request)).isNull();
		verify(formUsageProvider, never()).checkSessionUser(any());
	}

	@Test
	void deleteForm_removesTheForm() {
		service.deleteForm("f-1", request);

		verify(formProvider).delete("f-1");
	}

	@Test
	void findFormById_servesTheSchemaAsJson() {
		when(formProvider.findById("f-1")).thenReturn(Optional.of(form("f-1", "invoice-form")));

		ResponseEntity<byte[]> response = service.findFormById("f-1", request);

		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(response.getBody()).isEqualTo("{}".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void findFormById_answers404WhenItIsGone() {
		when(formProvider.findById("missing")).thenReturn(Optional.empty());

		assertThat(service.findFormById("missing", request).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void findByFormId_returnsTheForm() {
		when(formProvider.findByFormId("invoice-form")).thenReturn(form("f-1", "invoice-form"));

		assertThat(service.findByFormId("invoice-form", request).getBody().getId()).isEqualTo("f-1");
	}

	@Test
	void findByFormId_answers404WhenThereIsNoSuchForm() {
		when(formProvider.findByFormId("nope")).thenReturn(null);

		assertThat(service.findByFormId("nope", request).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// ---------- deployment ----------

	@Test
	void deployBpmn_delegatesToTheEngineProviderWithTheAuthenticatedUser() {
		org.springframework.util.MultiValueMap<String, Object> data =
			new org.springframework.util.LinkedMultiValueMap<>();
		org.springframework.util.MultiValueMap<String, MultipartFile> file =
			new org.springframework.util.LinkedMultiValueMap<>();

		service.deployBpmn(data, file, request);

		verify(bpmProvider).deployBpmn(data, file, user);
	}
}
