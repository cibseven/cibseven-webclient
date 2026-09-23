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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.modeler.model.ElementTemplate;
import org.cibseven.modeler.model.ElementTemplateOrigin;
import org.cibseven.modeler.provider.ElementTemplateProvider;
import org.cibseven.modeler.rest.dto.ElementTemplateRequest;
import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.ModelerAccessChecker;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.exception.NoObjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ElementTemplateServiceTest {

	@Mock
	private ElementTemplateProvider templateProvider;

	@Mock
	private ModelerAccessChecker modelerAccessChecker;

	@Mock
	@SuppressWarnings("rawtypes")
	private BaseUserProvider baseUserProvider;

	private ElementTemplateService service;
	private HttpServletRequest request;
	private CIBUser user;

	@BeforeEach
	void setUp() {
		service = new ElementTemplateService();
		ReflectionTestUtils.setField(service, "templateProvider", templateProvider);
		ReflectionTestUtils.setField(service, "modelerAccessChecker", modelerAccessChecker);
		ReflectionTestUtils.setField(service, "baseUserProvider", baseUserProvider);

		request = mock(HttpServletRequest.class);
		user = new CIBUser("demo");
		when(baseUserProvider.checkAuthorization(request, true)).thenReturn(user);
	}

	private ElementTemplate template(String id, String name, String templateId, boolean active,
			String createdBy, String description) {
		ElementTemplate template = new ElementTemplate();
		template.setId(id);
		template.setName(name);
		template.setTemplateId(templateId);
		template.setActive(active);
		template.setCreatedBy(createdBy);
		template.setDescription(description);
		template.setOrigin(ElementTemplateOrigin.MANUAL);
		return template;
	}

	// ---------- access control ----------

	@Test
	void getAllElementTemplates_needsAuthenticationButNotModelerRights() {
		when(templateProvider.getElementTemplates()).thenReturn(List.of());

		service.getAllElementTemplates(request);

		// the cockpit diagram viewer loads templates for icon rendering without modeler access
		verify(baseUserProvider).checkAuthorization(request, true);
		verify(modelerAccessChecker, never()).checkModelerAccess(user);
	}

	@Test
	void getAllElementTemplates_rejectsAnUnauthenticatedCaller() {
		when(baseUserProvider.checkAuthorization(request, true)).thenReturn(null);

		// the providers may return null rather than throwing, so the base service turns it into 401
		assertThatThrownBy(() -> service.getAllElementTemplates(request))
			.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void getElementTemplateById_requiresModelerAccess() throws Exception {
		when(templateProvider.findById("t-1"))
			.thenReturn(Optional.of(template("t-1", "Mail", "mail", true, "demo", null)));

		service.getElementTemplateById(request, "t-1");

		verify(modelerAccessChecker).checkModelerAccess(user);
	}

	@Test
	void getElementTemplateById_propagatesAnAccessDenial() {
		doThrow(new AccessDeniedException("no modeler")).when(modelerAccessChecker).checkModelerAccess(user);

		assertThatThrownBy(() -> service.getElementTemplateById(request, "t-1"))
			.isInstanceOf(AccessDeniedException.class);
		verify(templateProvider, never()).findById("t-1");
	}

	// ---------- read ----------

	@Test
	void getAllElementTemplates_returnsWhatTheProviderHas() {
		when(templateProvider.getElementTemplates())
			.thenReturn(List.of(template("t-1", "Mail", "mail", true, "demo", null)));

		assertThat(service.getAllElementTemplates(request))
			.singleElement().extracting(ElementTemplate::getName).isEqualTo("Mail");
	}

	@Test
	void getElementTemplateById_returnsTheTemplate() throws Exception {
		when(templateProvider.findById("t-1"))
			.thenReturn(Optional.of(template("t-1", "Mail", "mail", true, "demo", null)));

		assertThat(service.getElementTemplateById(request, "t-1").getId()).isEqualTo("t-1");
	}

	@Test
	void getElementTemplateById_throwsWhenTheTemplateIsUnknown() {
		when(templateProvider.findById("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getElementTemplateById(request, "missing"))
			.isInstanceOf(NoObjectException.class);
	}

	// ---------- create ----------

	@Test
	void add_stampsTheAuthenticatedUserAsCreator() {
		ElementTemplateRequest incoming = new ElementTemplateRequest();
		incoming.setName("Mail");
		incoming.setTemplateId("mail");
		incoming.setContent("{}");
		when(templateProvider.addTemplate(org.mockito.ArgumentMatchers.any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		ElementTemplate created = service.add(request, incoming);

		assertThat(created.getCreatedBy()).isEqualTo("demo");
		assertThat(created.getName()).isEqualTo("Mail");
	}

	@Test
	void add_defaultsTheOriginToManual() {
		ElementTemplateRequest incoming = new ElementTemplateRequest();
		incoming.setName("Mail");
		incoming.setTemplateId("mail");
		when(templateProvider.addTemplate(org.mockito.ArgumentMatchers.any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(service.add(request, incoming).getOrigin()).isEqualTo(ElementTemplateOrigin.MANUAL);
	}

	@Test
	void add_keepsAnExplicitOrigin() {
		ElementTemplateRequest incoming = new ElementTemplateRequest();
		incoming.setName("Mail");
		incoming.setOrigin(ElementTemplateOrigin.CUSTOM_JSON);
		when(templateProvider.addTemplate(org.mockito.ArgumentMatchers.any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(service.add(request, incoming).getOrigin()).isEqualTo(ElementTemplateOrigin.CUSTOM_JSON);
	}

	@Test
	void add_leavesActiveAtItsDefaultWhenTheRequestOmitsIt() {
		ElementTemplateRequest incoming = new ElementTemplateRequest();
		incoming.setName("Mail");
		when(templateProvider.addTemplate(org.mockito.ArgumentMatchers.any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		// the entity defaults to active, and an absent flag must not turn it off
		assertThat(service.add(request, incoming).getActive()).isTrue();
	}

	@Test
	void add_honoursAnExplicitInactiveFlag() {
		ElementTemplateRequest incoming = new ElementTemplateRequest();
		incoming.setName("Mail");
		incoming.setActive(Boolean.FALSE);
		when(templateProvider.addTemplate(org.mockito.ArgumentMatchers.any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(service.add(request, incoming).getActive()).isFalse();
	}

	// ---------- delete ----------

	@Test
	void delete_delegatesToTheProvider() {
		service.delete(request, "t-1");

		verify(templateProvider).deleteTemplateById("t-1");
	}

	@Test
	void bulkDelete_reportsWhichIdsSucceededAndWhichFailed() {
		doThrow(new IllegalStateException("still in use"))
			.when(templateProvider).deleteTemplateById("t-2");

		ResponseEntity<Map<String, Object>> response =
			service.bulkDelete(request, List.of("t-1", "t-2", "t-3"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, Object> body = response.getBody();
		// one bad id must not abort the whole batch
		assertThat(body).containsEntry("deleted", List.of("t-1", "t-3"))
			.containsEntry("failed", List.of("t-2"))
			.containsEntry("totalRequested", 3)
			.containsEntry("totalDeleted", 2);
	}

	// ---------- duplicate ----------

	@Test
	void duplicateTemplate_marksTheCopyAndGivesItAFreshTemplateId() throws Exception {
		ElementTemplate original = template("t-1", "Mail", "mail", true, "someone-else", "sends mail");
		when(templateProvider.findById("t-1")).thenReturn(Optional.of(original));
		when(templateProvider.addTemplate(org.mockito.ArgumentMatchers.any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		ElementTemplate copy = service.duplicateTemplate(request, "t-1");

		assertThat(copy.getName()).isEqualTo("Mail (Copy)");
		assertThat(copy.getTemplateId()).startsWith("mail_copy_").isNotEqualTo("mail");
		assertThat(copy.getDescription()).isEqualTo("sends mail");
		// the copy belongs to whoever made it, not to the original author
		assertThat(copy.getCreatedBy()).isEqualTo("demo");
	}

	@Test
	void duplicateTemplate_throwsWhenTheOriginalIsUnknown() {
		when(templateProvider.findById("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.duplicateTemplate(request, "missing"))
			.isInstanceOf(NoObjectException.class);
		verify(templateProvider, never()).addTemplate(org.mockito.ArgumentMatchers.any());
	}

	// ---------- search and filter ----------

	@Test
	void searchTemplates_withoutCriteriaReturnsEverything() {
		when(templateProvider.getElementTemplates()).thenReturn(List.of(
			template("t-1", "Mail", "mail", true, "demo", null),
			template("t-2", "Slack", "slack", false, "other", null)));

		assertThat(service.searchTemplates(request, null, null, null, null, null)).hasSize(2);
	}

	@Test
	void searchTemplates_matchesTheNameCaseInsensitivelyAndPartially() {
		when(templateProvider.getElementTemplates()).thenReturn(List.of(
			template("t-1", "Mail sender", "mail", true, "demo", null),
			template("t-2", "Slack", "slack", true, "demo", null)));

		assertThat(service.searchTemplates(request, "MAIL", null, null, null, null))
			.singleElement().extracting(ElementTemplate::getId).isEqualTo("t-1");
	}

	@Test
	void searchTemplates_filtersByActiveStatus() {
		when(templateProvider.getElementTemplates()).thenReturn(List.of(
			template("t-1", "Mail", "mail", true, "demo", null),
			template("t-2", "Slack", "slack", false, "demo", null)));

		assertThat(service.searchTemplates(request, null, null, Boolean.FALSE, null, null))
			.singleElement().extracting(ElementTemplate::getId).isEqualTo("t-2");
	}

	@Test
	void searchTemplates_filtersByCreatorAndToleratesTemplatesWithoutOne() {
		when(templateProvider.getElementTemplates()).thenReturn(List.of(
			template("t-1", "Mail", "mail", true, "demo", null),
			template("t-2", "Slack", "slack", true, null, null)));

		assertThat(service.searchTemplates(request, null, "demo", null, null, null))
			.singleElement().extracting(ElementTemplate::getId).isEqualTo("t-1");
	}

	@Test
	void searchTemplates_filtersByDescriptionAndToleratesTemplatesWithoutOne() {
		when(templateProvider.getElementTemplates()).thenReturn(List.of(
			template("t-1", "Mail", "mail", true, "demo", "sends mail"),
			template("t-2", "Slack", "slack", true, "demo", null)));

		assertThat(service.searchTemplates(request, null, null, null, null, "SENDS"))
			.singleElement().extracting(ElementTemplate::getId).isEqualTo("t-1");
	}

	@Test
	void searchTemplates_filtersByTemplateId() {
		when(templateProvider.getElementTemplates()).thenReturn(List.of(
			template("t-1", "Mail", "mail", true, "demo", null),
			template("t-2", "Slack", "slack", true, "demo", null)));

		assertThat(service.searchTemplates(request, null, null, null, "SLA", null))
			.singleElement().extracting(ElementTemplate::getId).isEqualTo("t-2");
	}

	@Test
	void filterTemplates_defaultsToActiveOnly() {
		when(templateProvider.getElementTemplates()).thenReturn(List.of(
			template("t-1", "Mail", "mail", true, "demo", null),
			template("t-2", "Slack", "slack", false, "demo", null)));

		assertThat(service.filterTemplates(request, Boolean.TRUE, null))
			.singleElement().extracting(ElementTemplate::getId).isEqualTo("t-1");
	}

	@Test
	void filterTemplates_includesInactiveWhenAsked() {
		when(templateProvider.getElementTemplates()).thenReturn(List.of(
			template("t-1", "Mail", "mail", true, "demo", null),
			template("t-2", "Slack", "slack", false, "demo", null)));

		assertThat(service.filterTemplates(request, Boolean.FALSE, null)).hasSize(2);
	}

	@Test
	void filterTemplates_ignoresABlankCreatorFilter() {
		when(templateProvider.getElementTemplates()).thenReturn(List.of(
			template("t-1", "Mail", "mail", true, "demo", null)));

		assertThat(service.filterTemplates(request, Boolean.TRUE, "   ")).hasSize(1);
	}
}
