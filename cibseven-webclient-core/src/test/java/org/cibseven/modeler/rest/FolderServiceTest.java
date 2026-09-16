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
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

	/** The listing is what a client calls first, so it is where the folder has to appear. */
	@Test
	void listingCreatesTheFolderAnEmptyInstallationHasNone() {
		when(folderProvider.findAll()).thenReturn(List.of(new FolderEntity()));

		assertThat(service.findAll(request)).hasSize(1);
		verify(folderProvider).defaultFolder();
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

	/** Every endpoint is behind the modeler permission, not only the ones that write. */
	@Test
	void refusesAUserWithoutModelerAccess() {
		doThrow(new AccessDeniedException("no modeler")).when(modelerAccessChecker).checkModelerAccess(USER);

		assertThatThrownBy(() -> service.findAll(request)).isInstanceOf(AccessDeniedException.class);
		verify(folderProvider, never()).findAll();
	}

	private static Map<String, String> body(String key, String value) {
		Map<String, String> folder = new HashMap<>();
		folder.put(key, value);
		return folder;
	}
}
