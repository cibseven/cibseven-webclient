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

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.provider.FolderProvider.FolderChange;
import org.cibseven.modeler.repository.FolderRepository;
import org.cibseven.modeler.repository.FormRepository;
import org.cibseven.modeler.repository.ProcessDiagramRepository;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.exception.InvalidFolderException;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What the folder tree allows. The rules are here rather than in the database, because a name
 * taken twice or a folder moved into itself has to be reported as a request error naming the
 * field, not as a constraint violation.
 */
class FolderProviderTest {

	private FolderRepository folders;
	private ProcessDiagramRepository diagrams;
	private FormRepository forms;
	private FolderProvider provider;

	private FolderEntity project;

	@BeforeEach
	void setUp() {
		folders = Mockito.mock(FolderRepository.class);
		diagrams = Mockito.mock(ProcessDiagramRepository.class);
		forms = Mockito.mock(FormRepository.class);
		provider = new FolderProvider();
		ReflectionTestUtils.setField(provider, "folderDao", folders);
		ReflectionTestUtils.setField(provider, "processDiagramDao", diagrams);
		ReflectionTestUtils.setField(provider, "formDao", forms);

		project = folder("project", null, "Invoicing");
		when(folders.save(any())).thenAnswer(call -> call.getArgument(0));
		when(folders.findByParentIdOrderByNameAsc(any())).thenReturn(List.of());
	}

	private FolderEntity folder(String id, String parentId, String name) {
		FolderEntity folder = new FolderEntity();
		folder.setId(id);
		folder.setParentId(parentId);
		folder.setName(name);
		when(folders.findById(id)).thenReturn(Optional.of(folder));
		return folder;
	}

	@Test
	void createsAFolderInsideAnother() {
		FolderEntity created = provider.create("project", "  Drafts  ", "demo");

		assertThat(created.getName()).isEqualTo("Drafts");
		assertThat(created.getParentId()).isEqualTo("project");
		assertThat(created.getCreatedBy()).isEqualTo("demo");
	}

	@Test
	void refusesAFolderWithoutAName() {
		assertThatThrownBy(() -> provider.create("project", "   ", "demo"))
			.isInstanceOf(InvalidFolderException.class)
			.satisfies(thrown -> assertThat(((InvalidFolderException) thrown).getField()).isEqualTo("name"));
	}

	/** Two folders with one name in one place would be indistinguishable in the tree. */
	@Test
	void refusesASecondFolderWithTheSameNameBesideIt() {
		FolderEntity taken = folder("taken", "project", "Drafts");
		when(folders.findByParentIdAndName("project", "Drafts")).thenReturn(Optional.of(taken));

		assertThatThrownBy(() -> provider.create("project", "Drafts", "demo"))
			.isInstanceOf(InvalidFolderException.class);
		verify(folders, never()).save(any());
	}

	@Test
	void reportsAFolderThatIsNotThere() {
		when(folders.findById("gone")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> provider.find("gone")).isInstanceOf(NoObjectFoundException.class);
	}

	@Test
	void renamesAFolderWithoutTouchingItsId() {
		FolderEntity renamed = provider.rename("project", "Billing", "demo");

		assertThat(renamed.getId()).isEqualTo("project");
		assertThat(renamed.getName()).isEqualTo("Billing");
		assertThat(renamed.getUpdatedBy()).isEqualTo("demo");
	}

	@Test
	void movesAFolderKeepingItsId() {
		FolderEntity other = folder("other", null, "Archive");

		FolderEntity moved = provider.move("project", other.getId(), "demo");

		assertThat(moved.getId()).isEqualTo("project");
		assertThat(moved.getParentId()).isEqualTo("other");
	}

	/** A folder moved below itself would take its whole subtree out of the tree. */
	@Test
	void refusesToMoveAFolderIntoItself() {
		FolderEntity child = folder("child", "project", "Drafts");
		when(folders.findByParentIdOrderByNameAsc("project")).thenReturn(List.of(child));

		assertThatThrownBy(() -> provider.move("project", "child", "demo"))
			.isInstanceOf(InvalidFolderException.class)
			.satisfies(thrown -> assertThat(((InvalidFolderException) thrown).getField()).isEqualTo("parentId"));
	}

	@Test
	void countsWhatAFolderHoldsThroughTheWholeSubtree() {
		FolderEntity child = folder("child", "project", "Drafts");
		when(folders.findByParentIdOrderByNameAsc("project")).thenReturn(List.of(child));
		when(diagrams.countByFolderIdIn(anyList())).thenReturn(3L);
		when(forms.countByFolderIdIn(anyList())).thenReturn(2L);

		FolderProvider.FolderContents contents = provider.contents("project");

		assertThat(contents.folders()).isEqualTo(1);
		assertThat(contents.diagrams()).isEqualTo(3);
		assertThat(contents.forms()).isEqualTo(2);
		assertThat(contents.models()).isEqualTo(5);
	}

	@Test
	void deletesTheSubtreeAndReportsWhatWentWithIt() {
		FolderEntity child = folder("child", "project", "Drafts");
		when(folders.findByParentIdOrderByNameAsc("project")).thenReturn(List.of(child));
		when(diagrams.countByFolderIdIn(anyList())).thenReturn(1L);
		when(forms.countByFolderIdIn(anyList())).thenReturn(0L);
		when(diagrams.findByFolderIdIn(anyList())).thenReturn(List.of());
		when(forms.findByFolderIdIn(anyList())).thenReturn(List.of());

		FolderProvider.FolderContents removed = provider.delete("project", () -> false);

		assertThat(removed.folders()).isEqualTo(1);
		assertThat(removed.models()).isEqualTo(1);
		// A folder points at its parent, so the child has to go first
		verify(folders).deleteAllById(List.of("child", "project"));
	}

	@Test
	void acceptsAnExistingFolderForAModel() {
		assertThat(provider.requireModelFolder("project").getId()).isEqualTo("project");
	}

	@Test
	void createsAFolderAtTheTopLevel() {
		FolderEntity created = provider.create(null, "Archive", "demo");

		assertThat(created.getParentId()).isNull();
		assertThat(created.getName()).isEqualTo("Archive");
	}

	/**
	 * The top level is the one place the unique key cannot cover, because a null parent is not
	 * equal to itself on most databases: without this check a second Invoicing would be stored.
	 */
	@Test
	void refusesASecondTopLevelFolderWithTheSameName() {
		when(folders.findFirstByParentIdIsNullAndNameOrderByCreatedAsc("Invoicing")).thenReturn(Optional.of(project));

		assertThatThrownBy(() -> provider.create(null, "Invoicing", "demo"))
			.isInstanceOf(InvalidFolderException.class)
			.satisfies(thrown -> assertThat(((InvalidFolderException) thrown).getField()).isEqualTo("name"));
		verify(folders, never()).save(any());
	}

	@Test
	void movesAFolderToTheTopLevel() {
		folder("child", "project", "Drafts");

		FolderEntity moved = provider.move("child", null, "demo");

		assertThat(moved.getParentId()).isNull();
	}

	/**
	 * The schema creates the first folder and files the models of an upgraded installation into
	 * it. From there the tree is the user's, so a model that names no folder is refused rather
	 * than filed into one the backend brings back.
	 */
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " " })
	void reportsAMissingFolderForAModelAsARequestError(String folderId) {
		assertThatThrownBy(() -> provider.requireModelFolder(folderId))
			.isInstanceOf(InvalidFolderException.class)
			.satisfies(thrown -> assertThat(((InvalidFolderException) thrown).getField()).isEqualTo("folderId"));
		verify(folders, never()).save(any());
	}

	@Test
	void reportsALookupWithoutAnIdAsNotFound() {
		assertThatThrownBy(() -> provider.find(null)).isInstanceOf(NoObjectFoundException.class);
	}

	private FolderEntity userFolder(String user) {
		FolderEntity userFolder = folder("user-" + user, null, user);
		userFolder.setCreatedBy(user);
		return userFolder;
	}

	@Test
	void createsTheFolderOfAUserOnTheFirstVisit() {
		FolderEntity created = provider.userFolder("demo");

		assertThat(created.getParentId()).isNull();
		assertThat(created.getName()).isEqualTo("demo");
		assertThat(created.getCreatedBy()).isEqualTo("demo");
		assertThat(created.isUserFolder()).isTrue();
	}

	@Test
	void findsTheUserFolderOnLaterVisitsInsteadOfCreatingAnother() {
		FolderEntity existing = userFolder("demo");
		when(folders.findFirstByParentIdIsNullAndNameAndCreatedByOrderByCreatedAsc("demo", "demo"))
			.thenReturn(Optional.of(existing));

		assertThat(provider.userFolder("demo")).isSameAs(existing);
		verify(folders, never()).save(any());
	}

	@Test
	void isAUserFolderOnlyAtTheTopLevelAndUnderTheNameOfItsCreator() {
		FolderEntity nested = folder("nested", "project", "demo");
		nested.setCreatedBy("demo");
		project.setCreatedBy("demo");

		assertThat(userFolder("demo").isUserFolder()).isTrue();
		assertThat(nested.isUserFolder()).isFalse();
		assertThat(project.isUserFolder()).isFalse();
	}

	@Test
	void refusesToDeleteAUserFolderForAUserWhoMayNotManageUserFolders() {
		userFolder("demo");

		assertThatThrownBy(() -> provider.delete("user-demo", () -> false))
			.isInstanceOf(AccessDeniedException.class);
		verify(folders, never()).deleteAllById(any());
	}

	@Test
	void refusesToRenameOrMoveAUserFolderForAUserWhoMayNotManageUserFolders() {
		FolderEntity userFolder = userFolder("demo");

		assertThatThrownBy(() -> provider.update("user-demo", new FolderChange(true, "Mine", false, null), "demo", () -> false))
			.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> provider.update("user-demo", new FolderChange(false, null, true, "project"), "demo", () -> false))
			.isInstanceOf(AccessDeniedException.class);
		assertThat(userFolder.getName()).isEqualTo("demo");
		assertThat(userFolder.getParentId()).isNull();
		verify(folders, never()).save(any());
	}

	/** An administrator cleans up the folder of somebody who left. */
	@Test
	void letsAnAdministratorDeleteAUserFolder() {
		userFolder("demo");
		when(diagrams.findByFolderIdIn(anyList())).thenReturn(List.of());
		when(forms.findByFolderIdIn(anyList())).thenReturn(List.of());

		provider.delete("user-demo", () -> true);

		verify(folders).deleteAllById(List.of("user-demo"));
	}

	/** Whether the caller is an administrator costs a call to the engine, so only a user folder asks. */
	@Test
	void keepsWhatIsInsideAUserFolderChangeableWithoutAskingForAdministrators() {
		folder("drafts", "user-demo", "Drafts");
		userFolder("demo");
		BooleanSupplier notToBeAsked = () -> {
			throw new AssertionError("asked whether the caller may manage user folders for an ordinary folder");
		};
		when(diagrams.findByFolderIdIn(anyList())).thenReturn(List.of());
		when(forms.findByFolderIdIn(anyList())).thenReturn(List.of());

		FolderEntity renamed = provider.update("drafts", new FolderChange(true, "Sketches", false, null), "demo", notToBeAsked);
		provider.delete("drafts", notToBeAsked);

		assertThat(renamed.getName()).isEqualTo("Sketches");
		verify(folders).deleteAllById(List.of("drafts"));
	}

	private ProjectAccess watchedAccess() {
		ProjectAccess access = Mockito.mock(ProjectAccess.class);
		ReflectionTestUtils.setField(provider, "projectAccess", access);
		return access;
	}

	@Test
	void findsTheProjectOfEveryFolderAtAnyDepth() {
		FolderEntity drafts = folder("drafts", "project", "Drafts");
		FolderEntity old = folder("old", "drafts", "Old");
		FolderEntity archive = folder("archive", null, "Archive");
		when(folders.findAll()).thenReturn(List.of(project, drafts, old, archive));

		assertThat(provider.projectsByFolder())
			.containsEntry("project", "project")
			.containsEntry("drafts", "project")
			.containsEntry("old", "project")
			.containsEntry("archive", "archive");
		assertThat(provider.projectOf("old")).isEqualTo("project");
		assertThat(provider.projectOf("gone")).isNull();
		assertThat(provider.projectOf(null)).isNull();
	}

	/** A broken chain would otherwise walk for ever; the folder whose parent is gone counts as the top. */
	@Test
	void stopsAtAFolderWhoseParentIsGone() {
		FolderEntity orphan = folder("orphan", "deleted", "Orphan");
		FolderEntity a = folder("a", "b", "A");
		FolderEntity b = folder("b", "a", "B");
		when(folders.findAll()).thenReturn(List.of(orphan, a, b));

		assertThat(provider.projectOf("orphan")).isEqualTo("orphan");
		assertThat(provider.projectOf("a")).isNotNull();
	}

	@Test
	void makesTheCreatorTheFirstAdminOfANewProjectOnly() {
		ProjectAccess access = watchedAccess();

		FolderEntity created = provider.create(null, "Archive", "demo");
		provider.create("project", "Drafts", "demo");

		verify(access).projectCreated("demo", created);
		verify(access, Mockito.times(1)).projectCreated(any(), any());
	}

	/** A user folder from before there were members is claimed by its owner on the next visit. */
	@Test
	void givesAnExistingUserFolderItsOwner() {
		ProjectAccess access = watchedAccess();
		FolderEntity existing = userFolder("demo");
		when(folders.findFirstByParentIdIsNullAndNameAndCreatedByOrderByCreatedAsc("demo", "demo"))
			.thenReturn(Optional.of(existing));

		provider.userFolder("demo");

		verify(access).projectCreated("demo", existing);
	}

	@Test
	void handsAFolderTakenOutToTheTopLevelTheMembersOfItsFormerProject() {
		ProjectAccess access = watchedAccess();
		FolderEntity drafts = folder("drafts", "project", "Drafts");
		FolderEntity old = folder("old", "drafts", "Old");
		when(folders.findAll()).thenReturn(List.of(project, drafts, old));

		FolderEntity moved = provider.move("old", null, "demo");

		verify(access).becameProject("demo", moved, "project");
	}

	@Test
	void asksNothingWhenAFolderStaysInsideItsProject() {
		ProjectAccess access = watchedAccess();
		folder("drafts", "project", "Drafts");
		folder("old", "drafts", "Old");

		provider.move("old", "project", "demo");

		verify(access, never()).becameProject(any(), any(), any());
	}

	/** A folder at the top level is its own project, so it learns it once its id exists. */
	@Test
	void makesANewFolderAtTheTopLevelItsOwnProject() {
		when(folders.save(any())).thenAnswer(call -> {
			FolderEntity saved = call.getArgument(0);
			if (saved.getId() == null) {
				saved.setId("new");
			}
			return saved;
		});

		assertThat(provider.create(null, "Archive", "demo").getProjectId()).isEqualTo("new");
	}

	@Test
	void putsANewFolderIntoTheProjectOfItsParent() {
		project.setProjectId("project");
		FolderEntity drafts = folder("drafts", "project", "Drafts");
		drafts.setProjectId("project");

		assertThat(provider.create("drafts", "Old", "demo").getProjectId()).isEqualTo("project");
	}

	/** A parent from before the project was kept has none stored; the tree tells it. */
	@Test
	void asksTheTreeForTheProjectOfAParentWithoutOne() {
		FolderEntity drafts = folder("drafts", "project", "Drafts");
		when(folders.findAll()).thenReturn(List.of(project, drafts));

		assertThat(provider.create("drafts", "Old", "demo").getProjectId()).isEqualTo("project");
	}

	/** Moved into another project, the folder takes everything below it along. */
	@Test
	void movesTheWholeSubtreeIntoTheProjectOfTheTarget() {
		project.setProjectId("project");
		FolderEntity drafts = folder("drafts", "project", "Drafts");
		drafts.setProjectId("project");
		FolderEntity archive = folder("archive", null, "Archive");
		archive.setProjectId("archive");
		FolderEntity old = folder("old", "drafts", "Old");
		when(folders.findByParentIdOrderByNameAsc("drafts")).thenReturn(List.of(old));

		FolderEntity moved = provider.move("drafts", "archive", "demo");

		assertThat(moved.getProjectId()).isEqualTo("archive");
		verify(folders).setProject(List.of("drafts", "old"), "archive");
	}

	@Test
	void makesAFolderTakenOutToTheTopLevelAProjectWithEverythingBelowIt() {
		project.setProjectId("project");
		FolderEntity drafts = folder("drafts", "project", "Drafts");
		drafts.setProjectId("project");
		when(folders.findAll()).thenReturn(List.of(project, drafts));

		FolderEntity moved = provider.move("drafts", null, "demo");

		assertThat(moved.getProjectId()).isEqualTo("drafts");
		verify(folders).setProject(List.of("drafts"), "drafts");
	}

	@Test
	void rewritesNothingWhenAFolderStaysInItsProject() {
		project.setProjectId("project");
		FolderEntity drafts = folder("drafts", "project", "Drafts");
		drafts.setProjectId("project");
		folder("old", "drafts", "Old").setProjectId("project");

		provider.move("old", "project", "demo");

		verify(folders, never()).setProject(any(), any());
	}

	/** A database takes only so many values in one list, so a large subtree goes in slices. */
	@Test
	void rewritesALargeSubtreeInSlices() {
		FolderEntity archive = folder("archive", null, "Archive");
		archive.setProjectId("archive");
		List<FolderEntity> children = IntStream.range(0, ProjectGuard.QUERY_LIMIT + 100).mapToObj(i -> {
			FolderEntity child = new FolderEntity();
			child.setId("child-" + i);
			child.setParentId("project");
			return child;
		}).toList();
		when(folders.findByParentIdOrderByNameAsc("project")).thenReturn(children);

		provider.move("project", "archive", "demo");

		verify(folders, times(2)).setProject(any(), eq("archive"));
		verify(folders).setProject(argThat(ids -> ids.size() == ProjectGuard.QUERY_LIMIT), eq("archive"));
		verify(folders).setProject(argThat(ids -> ids.size() == 101), eq("archive"));
	}

	/** Folders from before the project was kept have none, and a stored one may be stale. */
	@Test
	void storesTheProjectOfEveryFolderThatHasAWrongOrNoOne() {
		FolderEntity drafts = folder("drafts", "project", "Drafts");
		drafts.setProjectId("elsewhere");
		FolderEntity archive = folder("archive", null, "Archive");
		archive.setProjectId("archive");
		when(folders.findAll()).thenReturn(List.of(project, drafts, archive));

		assertThat(provider.fillProjects()).isEqualTo(2);

		assertThat(project.getProjectId()).isEqualTo("project");
		assertThat(drafts.getProjectId()).isEqualTo("project");
		verify(folders).saveAll(List.of(project, drafts));
	}
}
