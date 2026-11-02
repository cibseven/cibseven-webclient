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
import java.util.function.BooleanSupplier;

import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.provider.FolderProvider;
import org.cibseven.modeler.provider.FolderProvider.FolderChange;
import org.cibseven.modeler.provider.FolderProvider.FolderContents;
import org.cibseven.modeler.provider.ProjectAccess.Action;
import org.cibseven.modeler.provider.ProjectGuard;
import org.cibseven.modeler.provider.ProjectGuard.AccessOverview;
import org.cibseven.webapp.auth.AuthorizationChecker;
import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.ModelerAccessChecker;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
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
	private AuthorizationChecker authorizationChecker;
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
		authorizationChecker = mock(AuthorizationChecker.class);
		ReflectionTestUtils.setField(service, "authorizationChecker", authorizationChecker);
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

		verify(folderProvider).update(eq("folder-1"), eq(new FolderChange(true, "Billing", false, null)), eq("demo"), any(BooleanSupplier.class));
	}

	/** A parentId that is there but null is the request to take the folder out of its parent. */
	@Test
	void movesToTheTopLevelWhenTheParentIsNull() {
		Map<String, String> folder = new HashMap<>();
		folder.put("parentId", null);

		service.update("folder-1", folder, request);

		verify(folderProvider).update(eq("folder-1"), eq(new FolderChange(false, null, true, null)), eq("demo"), any(BooleanSupplier.class));
	}

	/** Both in one request, so the provider can apply them as one. */
	@Test
	void passesARenameAndAMoveOnTogether() {
		Map<String, String> folder = new HashMap<>();
		folder.put("name", "Billing");
		folder.put("parentId", "folder-2");

		service.update("folder-1", folder, request);

		verify(folderProvider).update(eq("folder-1"), eq(new FolderChange(true, "Billing", true, "folder-2")), eq("demo"), any(BooleanSupplier.class));
	}

	@Test
	void deletesTheFolderWithWhatItHolds() {
		when(folderProvider.delete(eq("folder-1"), any(BooleanSupplier.class))).thenReturn(new FolderContents(1, 2, 3));

		assertThat(service.delete("folder-1", request).models()).isEqualTo(5);
	}

	@Test
	void handsTheCallerTheirFolder() {
		FolderEntity userFolder = new FolderEntity();
		when(folderProvider.userFolder("demo")).thenReturn(userFolder);

		FolderService.UserFolder result = service.userFolder(request);

		assertThat(result.folder()).isSameAs(userFolder);
		assertThat(result.manageUserFolders()).isFalse();
	}

	/** Holding ALL on the system resource is what makes an administrator here. */
	@Test
	void letsAnAdministratorManageUserFolders() {
		when(authorizationChecker.isAuthorized(USER, SevenResourceType.SYSTEM, "*", "ALL")).thenReturn(true);

		assertThat(service.userFolder(request).manageUserFolders()).isTrue();
		assertThat(askedOnDelete().getAsBoolean()).isTrue();
		assertThat(askedOnUpdate().getAsBoolean()).isTrue();
	}

	@Test
	void tellsTheProviderWhenTheCallerMayNotManageUserFolders() {
		assertThat(askedOnDelete().getAsBoolean()).isFalse();
		assertThat(askedOnUpdate().getAsBoolean()).isFalse();
	}

	/** Asking the engine is left to the provider, which only does it for a user folder. */
	@Test
	void doesNotAskTheEngineBeforeTheProviderNeedsIt() {
		service.delete("folder-1", request);
		service.update("folder-1", body("name", "Billing"), request);

		verifyNoInteractions(authorizationChecker);
	}

	private BooleanSupplier askedOnDelete() {
		ArgumentCaptor<BooleanSupplier> manageUserFolders = ArgumentCaptor.forClass(BooleanSupplier.class);
		service.delete("folder-1", request);
		verify(folderProvider).delete(eq("folder-1"), manageUserFolders.capture());
		return manageUserFolders.getValue();
	}

	private BooleanSupplier askedOnUpdate() {
		ArgumentCaptor<BooleanSupplier> manageUserFolders = ArgumentCaptor.forClass(BooleanSupplier.class);
		service.update("folder-1", body("name", "Billing"), request);
		verify(folderProvider).update(eq("folder-1"), eq(new FolderChange(true, "Billing", false, null)), eq("demo"),
			manageUserFolders.capture());
		return manageUserFolders.getValue();
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

		assertEquals(8, checked, "endpoints of FolderService covered by this sweep");
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

	private ProjectGuard guarded() {
		ProjectGuard guard = mock(ProjectGuard.class);
		ReflectionTestUtils.setField(service, "projectGuard", guard);
		return guard;
	}

	private FolderEntity known(String id, String parentId) {
		FolderEntity folder = new FolderEntity();
		folder.setId(id);
		folder.setParentId(parentId);
		when(folderProvider.find(id)).thenReturn(folder);
		return folder;
	}

	@Test
	void listsOnlyTheFoldersTheCallerMaySee() {
		ProjectGuard guard = guarded();
		List<FolderEntity> all = List.of(new FolderEntity(), new FolderEntity());
		List<FolderEntity> visible = List.of(all.get(0));
		when(folderProvider.findAll()).thenReturn(all);
		when(guard.visible(USER, all)).thenReturn(visible);

		assertThat(service.findAll(request)).isSameAs(visible);
	}

	@Test
	void handsTheCallerTheirRightsPerProject() {
		ProjectGuard guard = guarded();
		AccessOverview overview = new AccessOverview(Map.of(), false);
		when(guard.overview(USER)).thenReturn(overview);

		assertThat(service.access(request)).isSameAs(overview);
	}

	/** Without an edition of its own the guard is open: no rights to list, and moves go anywhere. */
	@Test
	void reportsEveryProjectOpenWithoutAnEdition() {
		assertThat(service.access(request)).isEqualTo(new AccessOverview(Map.of(), true));
	}

	@Test
	void asksBeforeReadingAFolder() {
		ProjectGuard guard = guarded();
		doThrow(new AccessDeniedException("not a member")).when(guard).require(USER, "folder-1", Action.VIEW);

		assertThatThrownBy(() -> service.find("folder-1", request)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> service.contents("folder-1", request)).isInstanceOf(AccessDeniedException.class);
		verify(folderProvider, never()).find(any());
		verify(folderProvider, never()).contents(any());
	}

	/** Anyone may start a project; a folder inside one is up to its editors. */
	@Test
	void asksTheProjectOnlyForAFolderInsideIt() {
		ProjectGuard guard = guarded();

		service.create(body("name", "Claims"), request);
		service.create(Map.of("name", "Drafts", "parentId", "claims"), request);

		verify(guard).require(USER, "claims", Action.EDIT);
		verify(guard, Mockito.times(1)).require(any(), any(), any());
	}

	@Test
	void asksBeforeRenamingMovingOrDeleting() {
		ProjectGuard guard = guarded();
		FolderEntity folder = known("folder-1", "claims");
		Map<String, String> toTop = new HashMap<>();
		toTop.put("parentId", null);

		service.update("folder-1", body("name", "Billing"), request);
		service.update("folder-1", toTop, request);
		service.update("folder-1", body("parentId", "drafts"), request);
		service.delete("folder-1", request);

		verify(guard, Mockito.times(2)).requireFolderChange(USER, folder);
		verify(guard).requireFolderMove(USER, folder, null);
		verify(guard).requireFolderMove(USER, folder, "drafts");
	}

	@Test
	void changesNothingWhenTheGuardRefuses() {
		ProjectGuard guard = guarded();
		FolderEntity folder = known("folder-1", "claims");
		doThrow(new AccessDeniedException("viewer")).when(guard).requireFolderChange(USER, folder);

		assertThatThrownBy(() -> service.delete("folder-1", request)).isInstanceOf(AccessDeniedException.class);
		verify(folderProvider, never()).delete(any(), any());
	}
}
