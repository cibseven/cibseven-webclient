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

import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.repository.FolderRepository;
import org.cibseven.modeler.repository.FormRepository;
import org.cibseven.modeler.repository.ProcessDiagramRepository;
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
import static org.mockito.Mockito.never;
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

		FolderProvider.FolderContents removed = provider.delete("project");

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
		when(folders.findByParentIdIsNullAndName("Invoicing")).thenReturn(Optional.of(project));

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
}
