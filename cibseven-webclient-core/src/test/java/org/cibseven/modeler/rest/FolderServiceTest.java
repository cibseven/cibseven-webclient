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

import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.provider.FolderProvider;
import org.cibseven.modeler.provider.FolderProvider.FolderChange;
import org.cibseven.modeler.provider.FolderProvider.FolderContents;
import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.ModelerAccessChecker;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** What the folder endpoints pass on, and what they refuse before they get that far. */
class FolderServiceTest {

	private static final CIBUser USER = new CIBUser("demo");

	private FolderService service;
	private FolderProvider folderProvider;
	private ModelerAccessChecker modelerAccessChecker;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() throws Exception {
		service = new FolderService();
		folderProvider = mock(FolderProvider.class);
		modelerAccessChecker = mock(ModelerAccessChecker.class);
		request = mock(HttpServletRequest.class);
		BaseUserProvider<?> baseUserProvider = mock(BaseUserProvider.class);
		when(baseUserProvider.checkAuthorization(any(), anyBoolean())).thenReturn(USER);

		ReflectionTestUtils.setField(service, "folderProvider", folderProvider);
		ReflectionTestUtils.setField(service, "modelerAccessChecker", modelerAccessChecker);
		ReflectionTestUtils.setField(service, "baseUserProvider", baseUserProvider);
	}

	/** Reading the tree only reads: a folder the user removed is not put back by listing it. */
	@Test
	void listingHandsBackTheFoldersThereAreAndWritesNothing() {
		when(folderProvider.findAll()).thenReturn(List.of(new FolderEntity()));

		assertThat(service.findAll(request)).hasSize(1);
		verify(folderProvider, never()).create(any(), any(), any());
	}

	@Test
	void createsTheFolderTheRequestDescribes() {
		service.create(body("name", "Invoicing"), request);

		verify(folderProvider).create(null, "Invoicing", "demo");
	}

	@Test
	void renamesWithoutMovingWhenOnlyANameIsGiven() {
		service.update("folder-1", body("name", "Billing"), request);

		verify(folderProvider).update("folder-1", new FolderChange(true, "Billing", false, null), "demo");
	}

	/** A parentId that is there but null is the request to take the folder out of its parent. */
	@Test
	void movesToTheTopLevelWhenTheParentIsNull() {
		Map<String, String> folder = new HashMap<>();
		folder.put("parentId", null);

		service.update("folder-1", folder, request);

		verify(folderProvider).update("folder-1", new FolderChange(false, null, true, null), "demo");
	}

	/** Both in one request, so the provider can apply them as one. */
	@Test
	void passesARenameAndAMoveOnTogether() {
		Map<String, String> folder = new HashMap<>();
		folder.put("name", "Billing");
		folder.put("parentId", "folder-2");

		service.update("folder-1", folder, request);

		verify(folderProvider).update("folder-1", new FolderChange(true, "Billing", true, "folder-2"), "demo");
	}

	@Test
	void deletesTheFolderWithWhatItHolds() {
		when(folderProvider.delete("folder-1")).thenReturn(new FolderContents(1, 2, 3));

		assertThat(service.delete("folder-1", request).models()).isEqualTo(5);
	}

	@Test
	void readsOneFolderAndWhatItHolds() {
		FolderEntity folder = new FolderEntity();
		when(folderProvider.find("folder-1")).thenReturn(folder);
		when(folderProvider.contents("folder-1")).thenReturn(new FolderContents(0, 1, 0));

		assertThat(service.find("folder-1", request)).isSameAs(folder);
		assertThat(service.contents("folder-1", request).models()).isEqualTo(1);
	}

	/**
	 * Guards the whole controller rather than one endpoint: any endpoint added later that forgets
	 * the access check fails here, as in {@link ModelerServiceAuthorizationTest}.
	 */
	@Test
	void everyEndpointRejectsUsersWithoutModelerAccess() throws Exception {
		doThrow(new AccessDeniedException("no modeler")).when(modelerAccessChecker).checkModelerAccess(USER);
		List<String> unprotected = new ArrayList<>();
		int checked = 0;

		for (Method method : FolderService.class.getDeclaredMethods()) {
			if (!Modifier.isPublic(method.getModifiers())
					|| AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) == null) {
				continue;
			}
			checked++;
			try {
				method.invoke(service, defaultArguments(method));
				unprotected.add(method.getName());
			} catch (InvocationTargetException e) {
				if (!(e.getCause() instanceof AccessDeniedException)) {
					unprotected.add(method.getName() + " (" + e.getCause() + ")");
				}
			}
		}

		assertEquals(6, checked, "endpoints of FolderService covered by this sweep");
		assertEquals(List.of(), unprotected, "endpoints reachable without modeler access");
		verifyNoInteractions(folderProvider);
	}

	private Object[] defaultArguments(Method method) {
		Class<?>[] types = method.getParameterTypes();
		Object[] arguments = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			if (types[i] == HttpServletRequest.class) arguments[i] = request;
			else if (Map.class.isAssignableFrom(types[i])) arguments[i] = new HashMap<>();
			else if (types[i] == String.class) arguments[i] = "folder-1";
		}
		return arguments;
	}

	private static Map<String, String> body(String key, String value) {
		Map<String, String> folder = new HashMap<>();
		folder.put(key, value);
		return folder;
	}
}
