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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.cibseven.modeler.model.ElementTemplate;
import org.cibseven.modeler.provider.ElementTemplateProvider;
import org.cibseven.modeler.rest.dto.ElementTemplateRequest;
import org.cibseven.webapp.auth.AuthorizationChecker;
import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.ModelerAccessChecker;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.NoObjectException;
import org.cibseven.webapp.providers.BpmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The management screen edits the availability of a template together with its other fields, so the
 * full update has to carry the active flag instead of dropping it silently.
 */
class ElementTemplateActiveStateTest {

	private static final CIBUser USER = new CIBUser("demo");

	private ElementTemplateProvider templateProvider;
	private ElementTemplateService service;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() {
		templateProvider = Mockito.mock(ElementTemplateProvider.class);
		service = new ElementTemplateService();
		request = Mockito.mock(HttpServletRequest.class);

		BpmProvider bpmProvider = Mockito.mock(BpmProvider.class);
		BaseUserProvider<?> baseUserProvider = Mockito.mock(BaseUserProvider.class);
		ReflectionTestUtils.setField(service, "templateProvider", templateProvider);
		ReflectionTestUtils.setField(service, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(service, "baseUserProvider", baseUserProvider);
		ReflectionTestUtils.setField(service, "modelerAccessChecker",
			new ModelerAccessChecker(new AuthorizationChecker(bpmProvider)));

		when(baseUserProvider.checkAuthorization(any(), anyBoolean())).thenReturn(USER);
		when(bpmProvider.isUserAuthorized(USER, SevenResourceType.APPLICATION.getType(),
			ModelerAccessChecker.MODELER_RESOURCE_ID, ModelerAccessChecker.MODELER_PERMISSION)).thenReturn(true);
		when(templateProvider.updateTemplate(anyString(), any(ElementTemplate.class)))
			.thenAnswer(invocation -> invocation.getArgument(1));
		when(templateProvider.addTemplate(any(ElementTemplate.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
	}

	private ElementTemplateRequest request(Boolean active) {
		ElementTemplateRequest request = new ElementTemplateRequest();
		request.setName("One");
		request.setTemplateId("com.example.one");
		request.setDescription("first");
		request.setContent("{\"name\":\"One\"}");
		request.setActive(active);
		return request;
	}

	private ElementTemplate stored(boolean active) {
		ElementTemplate template = new ElementTemplate();
		template.setId("stored-id");
		template.setName("One");
		template.setTemplateId("com.example.one");
		template.setActive(active);
		when(templateProvider.findById("stored-id")).thenReturn(Optional.of(template));
		return template;
	}

	@Test
	void makesATemplateAvailableAgain() throws NoObjectException {
		stored(false);

		ElementTemplate updated = service.update(request, "stored-id", request(true));

		assertThat(updated.getActive()).isTrue();
	}

	@Test
	void hidesATemplate() throws NoObjectException {
		stored(true);

		ElementTemplate updated = service.update(request, "stored-id", request(false));

		assertThat(updated.getActive()).isFalse();
	}

	/** An older client that does not send the flag must not flip the stored state. */
	@Test
	void keepsTheStoredStateWhenTheRequestOmitsIt() throws NoObjectException {
		stored(false);

		ElementTemplate updated = service.update(request, "stored-id", request(null));

		assertThat(updated.getActive()).isFalse();
	}

	@Test
	void createsAHiddenTemplateWhenAsked() {
		ElementTemplate created = service.add(request, request(false));

		assertThat(created.getActive()).isFalse();
	}

	@Test
	void createdTemplatesAreAvailableByDefault() {
		ElementTemplate created = service.add(request, request(null));

		assertThat(created.getActive()).isTrue();
	}
}
