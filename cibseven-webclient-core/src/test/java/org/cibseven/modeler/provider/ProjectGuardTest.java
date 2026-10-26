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
package org.cibseven.modeler.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.provider.ProjectAccess.Action;
import org.cibseven.modeler.provider.ProjectAccess.ProjectRights;
import org.cibseven.modeler.provider.ProjectGuard.AccessOverview;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.InvalidFolderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** How a folder or a model is turned into the project that decides about it. */
class ProjectGuardTest {

	private static final CIBUser USER = new CIBUser("demo");

	private ProjectAccess access;
	private FolderProvider folders;
	private ProjectGuard guard;

	@BeforeEach
	void setUp() {
		access = Mockito.mock(ProjectAccess.class);
		folders = Mockito.mock(FolderProvider.class);
		guard = new ProjectGuard(access, folders);
		// Claims with drafts inside it, and Sales beside it
		when(folders.projectsByFolder()).thenReturn(Map.of(
			"claims", "claims", "drafts", "claims", "sales", "sales"));
		when(folders.projectOf(any())).thenAnswer(call -> Map.of(
			"claims", "claims", "drafts", "claims", "sales", "sales").get(call.getArgument(0)));
	}

	/** The community edition pays nothing: no folder is looked up and nothing is refused. */
	@Test
	void asksNothingWhenEveryProjectIsOpen() {
		ProjectGuard open = new ProjectGuard(new OpenProjectAccess(), folders);
		FolderEntity project = folder("claims", null);

		open.require(USER, "drafts", Action.MANAGE);
		open.requireFolderChange(USER, project);
		open.requireMove(USER, "drafts", "sales");
		List<FolderEntity> all = List.of(project);

		assertThat(open.visible(USER, all)).isSameAs(all);
		assertThat(open.hiddenFolderIds(USER)).isEmpty();
		assertThat(open.overview(USER)).isEqualTo(new AccessOverview(Map.of(), true));
		verifyNoInteractions(folders);
	}

	@Test
	void asksAboutTheProjectAFolderLiesIn() {
		guard.require(USER, "drafts", Action.EDIT);

		verify(access).require(USER, "claims", Action.EDIT);
	}

	/** A model from before there were folders is left to modeler access alone. */
	@Test
	void letsAModelWithoutAFolderThrough() {
		guard.require(USER, null, Action.EDIT);

		verify(access, never()).require(any(), any(), any());
	}

	@Test
	void leavesRenamingOrDeletingAProjectToItsAdmins() {
		guard.requireFolderChange(USER, folder("claims", null));
		guard.requireFolderChange(USER, folder("drafts", "claims"));

		verify(access).require(USER, "claims", Action.MANAGE);
		verify(access).require(USER, "claims", Action.EDIT);
	}

	@Test
	void letsAFolderMoveInsideItsProject() {
		guard.requireMove(USER, "drafts", "claims");

		verify(access, Mockito.times(2)).require(USER, "claims", Action.EDIT);
	}

	/** Taken out to the top level, the folder keeps its members: only the project it leaves is asked. */
	@Test
	void asksOnlyTheProjectLeftWhenAFolderBecomesAProject() {
		guard.requireMove(USER, "drafts", null);

		verify(access).require(USER, "claims", Action.EDIT);
		verify(access, Mockito.times(1)).require(any(), any(), any());
	}

	@Test
	void refusesAMoveIntoAnotherProjectWhenTheEditionDoes() {
		when(access.movesBetweenProjects()).thenReturn(false);

		assertThatThrownBy(() -> guard.requireMove(USER, "drafts", "sales"))
			.isInstanceOf(InvalidFolderException.class)
			.satisfies(thrown -> assertThat(((InvalidFolderException) thrown).getField()).isEqualTo("folderId"));
		// A project under another folder is a move between projects too
		assertThatThrownBy(() -> guard.requireMove(USER, "claims", "sales"))
			.isInstanceOf(InvalidFolderException.class);
	}

	@Test
	void letsAMoveIntoAnotherProjectThroughWhenTheEditionDoes() {
		when(access.movesBetweenProjects()).thenReturn(true);

		guard.requireMove(USER, "drafts", "sales");

		verify(access).require(USER, "claims", Action.EDIT);
		verify(access).require(USER, "sales", Action.EDIT);
	}

	@Test
	void hidesEveryFolderOfAHiddenProject() {
		when(access.hidden(eq(USER), anyCollection())).thenReturn(Set.of("claims"));

		assertThat(guard.hiddenFolderIds(USER)).containsExactlyInAnyOrder("claims", "drafts");
		assertThat(guard.visible(USER, List.of(folder("claims", null), folder("drafts", "claims"), folder("sales", null))))
			.extracting(FolderEntity::getId).containsExactly("sales");
	}

	@Test
	void reportsTheRightsInTheProjectsTheUserCanSee() {
		ProjectRights viewer = new ProjectRights("VIEWER", true, false, false);
		when(access.hidden(eq(USER), anyCollection())).thenReturn(Set.of("claims"));
		when(access.rights(eq(USER), anyCollection())).thenAnswer(call -> {
			Collection<String> asked = call.getArgument(1);
			assertThat(asked).containsExactly("sales");
			return Map.of("sales", viewer);
		});
		when(access.movesBetweenProjects()).thenReturn(false);

		assertThat(guard.overview(USER)).isEqualTo(new AccessOverview(Map.of("sales", viewer), false));
	}

	/** An empty NOT IN list is not valid SQL, and a blank stand-in is null on Oracle. */
	@Test
	void neverHandsAQueryAnEmptyListOfHiddenFolders() {
		assertThat(ProjectGuard.notEmpty(List.of())).containsExactly("-");
		assertThat(ProjectGuard.notEmpty(null)).containsExactly("-");
		assertThat(ProjectGuard.notEmpty(List.of("claims"))).containsExactly("claims");
	}

	/** Few hidden folders go into the query, which pages as it always did. */
	@Test
	void handsAShortListOfHiddenFoldersToTheQuery() {
		List<Object[]> asked = new ArrayList<>();
		ProjectGuard.PagedQuery<String> query = (hidden, page, size) -> {
			asked.add(new Object[] { hidden, page, size });
			return List.of("row");
		};

		assertThat(ProjectGuard.visiblePage(List.of("claims"), 40, 20, query, row -> null)).containsExactly("row");
		assertThat(asked).hasSize(1);
		assertThat(asked.get(0)).containsExactly(List.of("claims"), 2, 20);
	}

	/**
	 * Too many for one query on Oracle or SQL Server: the rows are read in batches and the hidden
	 * ones left out, and the page counts only what the user sees.
	 */
	@Test
	void leavesOutALongListOfHiddenFoldersWhileReadingTheRows() {
		List<String> hidden = IntStream.range(0, ProjectGuard.QUERY_LIMIT + 1).mapToObj(i -> "hidden-" + i).toList();
		// Every other row lies in a hidden folder: 600 rows, in three batches
		List<String[]> rows = IntStream.range(0, 600)
			.mapToObj(i -> new String[] { "row-" + i, i % 2 == 0 ? "hidden-" + (i % 100) : "visible" }).toList();
		List<Collection<String>> asked = new ArrayList<>();
		ProjectGuard.PagedQuery<String[]> query = (excluded, page, size) -> {
			asked.add(excluded);
			return rows.subList(Math.min(page * size, rows.size()), Math.min((page + 1) * size, rows.size()));
		};

		List<String[]> page = ProjectGuard.visiblePage(hidden, 100, 50, query, row -> row[1]);

		assertThat(page).extracting(row -> row[0]).first().isEqualTo("row-201");
		assertThat(page).hasSize(50).allSatisfy(row -> assertThat(row[1]).isEqualTo("visible"));
		assertThat(asked).allSatisfy(excluded -> assertThat(excluded).containsExactly("-"));
	}

	@Test
	void endsAShortPageWhenTheRowsRunOut() {
		List<String> hidden = IntStream.range(0, ProjectGuard.QUERY_LIMIT + 1).mapToObj(i -> "hidden-" + i).toList();
		ProjectGuard.PagedQuery<String[]> query = (excluded, page, size) -> page == 0
			? List.of(new String[] { "a", "visible" }, new String[] { "b", "hidden-3" }, new String[] { "c", null })
			: List.of();

		assertThat(ProjectGuard.visiblePage(hidden, 0, 20, query, row -> row[1]))
			.extracting(row -> row[0]).containsExactly("a", "c");
	}

	/** A request asks about several folders; the tree is read once for all of them. */
	@Test
	void readsTheTreeOncePerRequest() {
		RequestContextHolder.setRequestAttributes(
			new ServletRequestAttributes(new MockHttpServletRequest()));
		try {
			when(access.movesBetweenProjects()).thenReturn(false);
			guard.requireMove(USER, "drafts", "claims");
			guard.hiddenFolderIds(USER);

			verify(folders, Mockito.times(1)).projectsByFolder();
		} finally {
			RequestContextHolder.resetRequestAttributes();
		}
	}

	private static FolderEntity folder(String id, String parentId) {
		FolderEntity folder = new FolderEntity();
		folder.setId(id);
		folder.setParentId(parentId);
		folder.setName(id);
		return folder;
	}
}
