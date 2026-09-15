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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cibseven.modeler.model.ElementTemplate;
import org.cibseven.modeler.provider.ElementTemplateProvider;
import org.cibseven.modeler.rest.dto.ElementTemplateRequest;
import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.AuthorizationChecker;
import org.cibseven.webapp.auth.ModelerAccessChecker;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.providers.BpmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Element templates are modeler data as well, so the same access check applies — with one
 * deliberate exception: the plain listing stays open to any authenticated user because the
 * cockpit diagram viewer reads it to render element template icons.
 */
class ElementTemplateServiceAuthorizationTest {

	private static final CIBUser USER = new CIBUser("demo");

	/** The one endpoint the cockpit calls without ever opening the modeler. */
	private static final String AUTHENTICATION_ONLY_ENDPOINT = "getAllElementTemplates";

	private ElementTemplateService elementTemplateService;
	private BpmProvider bpmProvider;
	private BaseUserProvider<?> baseUserProvider;
	private ElementTemplateProvider templateProvider;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() {
		elementTemplateService = new ElementTemplateService();
		bpmProvider = mock(BpmProvider.class);
		baseUserProvider = mock(BaseUserProvider.class);
		templateProvider = mock(ElementTemplateProvider.class);
		request = mock(HttpServletRequest.class);

		ReflectionTestUtils.setField(elementTemplateService, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(elementTemplateService, "baseUserProvider", baseUserProvider);
		ReflectionTestUtils.setField(elementTemplateService, "templateProvider", templateProvider);
		ReflectionTestUtils.setField(elementTemplateService, "modelerAccessChecker", new ModelerAccessChecker(new AuthorizationChecker(bpmProvider)));

		when(baseUserProvider.checkAuthorization(any(), anyBoolean())).thenReturn(USER);
	}

	@Test
	void creatingTemplatesRequiresModelerAccess() {
		modelerAccess(false);

		assertThrows(AccessDeniedException.class,
			() -> elementTemplateService.add(request, new ElementTemplateRequest()));
		verify(templateProvider, never()).addTemplate(any());
	}

	@Test
	void deletingTemplatesRequiresModelerAccess() {
		modelerAccess(false);

		assertThrows(AccessDeniedException.class, () -> elementTemplateService.delete(request, "template-1"));
		verify(templateProvider, never()).deleteTemplateById(any());
	}

	@Test
	void bulkDeletingTemplatesRequiresModelerAccess() {
		modelerAccess(false);

		assertThrows(AccessDeniedException.class,
			() -> elementTemplateService.bulkDelete(request, List.of("template-1")));
		verify(templateProvider, never()).deleteTemplateById(any());
	}

	@Test
	void exportingTemplatesRequiresModelerAccess() {
		modelerAccess(false);

		assertThrows(AccessDeniedException.class, () -> elementTemplateService.exportTemplates(request, null, false));
		verify(templateProvider, never()).getElementTemplates();
	}

	@Test
	void listingTemplatesOnlyRequiresAuthentication() {
		modelerAccess(false);
		List<ElementTemplate> templates = List.of();
		when(templateProvider.getElementTemplates()).thenReturn(templates);

		assertSame(templates, elementTemplateService.getAllElementTemplates(request));
		verify(baseUserProvider).checkAuthorization(request, true);
	}

	@Test
	void userWithModelerAccessMayWrite() {
		modelerAccess(true);
		ElementTemplate created = new ElementTemplate();
		when(templateProvider.addTemplate(any())).thenReturn(created);

		assertSame(created, elementTemplateService.add(request, new ElementTemplateRequest()));
	}

	/**
	 * Any endpoint added later that forgets the access check fails here; the listing is the only
	 * documented exception.
	 */
	@Test
	void everyWritingEndpointRejectsUsersWithoutModelerAccess() {
		modelerAccess(false);
		List<String> unprotected = new ArrayList<>();
		int checked = 0;

		for (Method method : ElementTemplateService.class.getDeclaredMethods()) {
			if (!isEndpoint(method) || AUTHENTICATION_ONLY_ENDPOINT.equals(method.getName())) {
				continue;
			}
			checked++;
			try {
				method.invoke(elementTemplateService, defaultArguments(method));
				unprotected.add(method.getName());
			} catch (InvocationTargetException e) {
				if (!(e.getCause() instanceof AccessDeniedException)) {
					unprotected.add(method.getName() + " (" + e.getCause() + ")");
				}
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}

		assertEquals(14, checked, "endpoints of ElementTemplateService covered by this sweep");
		assertEquals(List.of(), unprotected, "endpoints reachable without modeler access");
	}

	private static boolean isEndpoint(Method method) {
		return Modifier.isPublic(method.getModifiers())
			&& (method.isAnnotationPresent(GetMapping.class)
				|| method.isAnnotationPresent(PostMapping.class)
				|| method.isAnnotationPresent(PutMapping.class)
				|| method.isAnnotationPresent(PatchMapping.class)
				|| method.isAnnotationPresent(DeleteMapping.class));
	}

	private Object[] defaultArguments(Method method) {
		Class<?>[] types = method.getParameterTypes();
		Object[] arguments = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			arguments[i] = defaultArgument(types[i]);
		}
		return arguments;
	}

	private Object defaultArgument(Class<?> type) {
		if (type == HttpServletRequest.class) {
			return request;
		}
		if (type == String.class) {
			return "x";
		}
		if (type == Boolean.class) {
			return Boolean.FALSE;
		}
		if (List.class.isAssignableFrom(type)) {
			return new ArrayList<>();
		}
		if (Map.class.isAssignableFrom(type)) {
			return new HashMap<>();
		}
		if (type == ElementTemplateRequest.class) {
			return new ElementTemplateRequest();
		}
		return mock(type, RETURNS_DEEP_STUBS);
	}

	private void modelerAccess(boolean granted) {
		when(bpmProvider.isUserAuthorized(USER, SevenResourceType.APPLICATION.getType(),
			ModelerAccessChecker.MODELER_RESOURCE_ID, ModelerAccessChecker.MODELER_PERMISSION)).thenReturn(granted);
	}

}
