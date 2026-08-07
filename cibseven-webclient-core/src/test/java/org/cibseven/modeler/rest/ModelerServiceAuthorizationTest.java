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
import java.util.Optional;

import org.cibseven.modeler.model.FormEntity;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.model.ProcessDiagramReduce;
import org.cibseven.modeler.provider.DBProcessDiagramProvider;
import org.cibseven.modeler.provider.DiagramUsageProvider;
import org.cibseven.modeler.provider.FormProvider;
import org.cibseven.modeler.provider.FormUsageProvider;
import org.cibseven.modeler.provider.UnifiedDiagramProvider;
import org.cibseven.modeler.provider.UserSessionProvider;
import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.AuthorizationChecker;
import org.cibseven.webapp.auth.ModelerAccessChecker;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.providers.BpmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The modeler keeps its diagrams and forms in the webclient database, so nothing but this check
 * stands between an authenticated user without modeler rights and read/write access to every
 * stored diagram (GitHub issue #1089).
 */
class ModelerServiceAuthorizationTest {

	private static final CIBUser USER = new CIBUser("demo");

	private ModelerService modelerService;
	private BpmProvider bpmProvider;
	private BaseUserProvider<?> baseUserProvider;
	private DBProcessDiagramProvider dbProcessDiagramProvider;
	private FormProvider formProvider;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() {
		modelerService = new ModelerService();
		bpmProvider = mock(BpmProvider.class);
		baseUserProvider = mock(BaseUserProvider.class);
		dbProcessDiagramProvider = mock(DBProcessDiagramProvider.class);
		formProvider = mock(FormProvider.class);
		request = mock(HttpServletRequest.class);

		ReflectionTestUtils.setField(modelerService, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(modelerService, "baseUserProvider", baseUserProvider);
		ReflectionTestUtils.setField(modelerService, "dbProcessDiagramProvider", dbProcessDiagramProvider);
		ReflectionTestUtils.setField(modelerService, "formProvider", formProvider);
		ReflectionTestUtils.setField(modelerService, "diagramUsageProvider", mock(DiagramUsageProvider.class));
		ReflectionTestUtils.setField(modelerService, "formUsageProvider", mock(FormUsageProvider.class));
		ReflectionTestUtils.setField(modelerService, "userSessionProvider", mock(UserSessionProvider.class));
		ReflectionTestUtils.setField(modelerService, "unifiedDiagramProvider", mock(UnifiedDiagramProvider.class));
		ReflectionTestUtils.setField(modelerService, "modelerAccessChecker", new ModelerAccessChecker(new AuthorizationChecker(bpmProvider)));

		when(baseUserProvider.checkAuthorization(any(), anyBoolean())).thenReturn(USER);
	}

	@Test
	void readingDiagramsRequiresModelerAccess() {
		modelerAccess(false);

		assertThrows(AccessDeniedException.class, () -> modelerService.getDiagrams(request, 0, 10, null, null));
		verify(dbProcessDiagramProvider, never()).getDiagrams(any(), any(), anyInt(), anyInt());
	}

	@Test
	void readingDiagramDataRequiresModelerAccess() {
		modelerAccess(false);

		assertThrows(AccessDeniedException.class, () -> modelerService.findByIdData("diagram-1", request));
		verify(dbProcessDiagramProvider, never()).findById(any());
	}

	@Test
	void writingDiagramsRequiresModelerAccess() {
		modelerAccess(false);
		ProcessDiagramEntity data = new ProcessDiagramEntity();
		data.setId("diagram-1");

		assertThrows(AccessDeniedException.class, () -> modelerService.saveProject(data, request));
		verify(dbProcessDiagramProvider, never()).createDiagram(any());
		verify(dbProcessDiagramProvider, never()).updateDiagram(any());
	}

	@Test
	void deletingDiagramsRequiresModelerAccess() {
		modelerAccess(false);

		assertThrows(AccessDeniedException.class, () -> modelerService.delete("diagram-1", request));
		verify(dbProcessDiagramProvider, never()).delete(any());
	}

	@Test
	void deletingFormsRequiresModelerAccess() {
		modelerAccess(false);

		assertThrows(AccessDeniedException.class, () -> modelerService.deleteForm("form-1", request));
		verify(formProvider, never()).delete(any());
	}

	/**
	 * The decision is the engine's: it can evaluate group and global grants without the caller
	 * needing READ on the authorization resource, which reading the authorizations cannot.
	 */
	@Test
	void theCheckIsDelegatedToTheEngine() {
		modelerAccess(true);

		modelerService.getDiagrams(request, 0, 10, null, null);

		verify(bpmProvider).isUserAuthorized(USER, SevenResourceType.APPLICATION.getType(),
			ModelerAccessChecker.MODELER_RESOURCE_ID, ModelerAccessChecker.MODELER_PERMISSION);
		verify(bpmProvider, never()).getUserAuthorization(any(), any());
	}

	/**
	 * Modeler endpoints always authenticate: there is no anonymous mode to fall back to, so an
	 * unresolvable caller is a 401 rather than a null user reaching the persistence layer.
	 */
	@Test
	void unauthenticatedCallersAreRejected() {
		when(baseUserProvider.checkAuthorization(any(), anyBoolean())).thenReturn(null);

		assertThrows(AuthenticationException.class, () -> modelerService.getDiagrams(request, 0, 10, null, null));
		verify(bpmProvider, never()).isUserAuthorized(any(), anyInt(), any(), any());
	}

	@Test
	void userWithModelerAccessIsServed() {
		modelerAccess(true);
		List<ProcessDiagramReduce> diagrams = new ArrayList<>();
		when(dbProcessDiagramProvider.getDiagrams(null, null, 0, 10)).thenReturn(diagrams);

		assertSame(diagrams, modelerService.getDiagrams(request, 0, 10, null, null));
	}

	@Test
	void writingDiagramsRecordsTheAuthorizedUser() {
		modelerAccess(true);
		ProcessDiagramEntity data = new ProcessDiagramEntity();
		data.setId("diagram-1");
		when(dbProcessDiagramProvider.findById("diagram-1")).thenReturn(Optional.empty());
		when(dbProcessDiagramProvider.createDiagram(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ProcessDiagramEntity created = modelerService.saveProject(data, request);

		assertEquals(USER.getUserID(), created.getUpdatedBy());
	}

	@Test
	void readingFormsIsServedForUserWithModelerAccess() {
		modelerAccess(true);
		FormEntity form = new FormEntity();
		form.setFormSchema(new byte[] { 1 });
		when(formProvider.findById("form-1")).thenReturn(Optional.of(form));

		assertEquals(200, modelerService.findFormById("form-1", request).getStatusCode().value());
	}

	/**
	 * Guards the whole controller rather than the handful of endpoints spelled out above: any
	 * endpoint added later that forgets the access check fails here.
	 */
	@Test
	void everyEndpointRejectsUsersWithoutModelerAccess() throws Exception {
		modelerAccess(false);
		List<String> unprotected = new ArrayList<>();
		int checked = 0;

		for (Method method : ModelerService.class.getDeclaredMethods()) {
			if (!method.isAnnotationPresent(RequestMapping.class) || !Modifier.isPublic(method.getModifiers())) {
				continue;
			}
			checked++;
			try {
				method.invoke(modelerService, defaultArguments(method));
				unprotected.add(method.getName());
			} catch (InvocationTargetException e) {
				if (!(e.getCause() instanceof AccessDeniedException)) {
					unprotected.add(method.getName() + " (" + e.getCause() + ")");
				}
			}
		}

		assertEquals(28, checked, "endpoints of ModelerService covered by this sweep");
		assertEquals(List.of(), unprotected, "endpoints reachable without modeler access");
	}

	@Test
	void deniedAccessNamesTheMissingPermission() {
		modelerAccess(false);

		AccessDeniedException exception = assertThrows(AccessDeniedException.class,
			() -> modelerService.getDiagrams(request, 0, 10, null, null));

		assertTrue(exception.getMessage().contains("ACCESS"), exception.getMessage());
		assertTrue(exception.getMessage().contains("modeler"), exception.getMessage());
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
		if (type == int.class) {
			return 0;
		}
		if (type == boolean.class) {
			return false;
		}
		if (type == String.class) {
			return "x";
		}
		if (MultiValueMap.class.isAssignableFrom(type)) {
			return new LinkedMultiValueMap<>();
		}
		if (Map.class.isAssignableFrom(type)) {
			return new HashMap<>();
		}
		if (type == MultipartFile.class) {
			return mock(MultipartFile.class);
		}
		if (type == ProcessDiagramEntity.class) {
			return new ProcessDiagramEntity();
		}
		return mock(type, RETURNS_DEEP_STUBS);
	}


	private void modelerAccess(boolean granted) {
		when(bpmProvider.isUserAuthorized(USER, SevenResourceType.APPLICATION.getType(),
			ModelerAccessChecker.MODELER_RESOURCE_ID, ModelerAccessChecker.MODELER_PERMISSION)).thenReturn(granted);
	}

}
