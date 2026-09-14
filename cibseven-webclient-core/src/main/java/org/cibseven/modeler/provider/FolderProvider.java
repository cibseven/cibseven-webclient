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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cibseven.modeler.config.ModelerJpa;
import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.model.ModelSource;
import org.cibseven.modeler.repository.FolderRepository;
import org.cibseven.modeler.repository.FormRepository;
import org.cibseven.modeler.repository.ProcessDiagramRepository;
import org.cibseven.webapp.exception.InvalidFolderException;
import org.cibseven.webapp.exception.NoObjectFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * The modeler folder tree: what may be created, renamed, moved and deleted, and what a folder
 * holds. A model belongs to exactly one folder, so the tree is also the path a model is reached
 * under - which is what the file and repository sources map onto later.
 */
@Component
@Slf4j
public class FolderProvider {

	/** The folder an upgrade puts the models of a flat installation in. */
	public static final String DEFAULT_FOLDER_NAME = "General";

	@Autowired
	private FolderRepository folderDao;

	@Autowired
	private ProcessDiagramRepository processDiagramDao;

	@Autowired
	private FormRepository formDao;

	/** What a folder holds, counted through the whole subtree. */
	public record FolderContents(long folders, long diagrams, long forms) {

		public long models() {
			return diagrams + forms;
		}
	}

	/**
	 * The root of a source, created on first use so an installation that never ran the migration
	 * still has somewhere to put its models.
	 */
	@Transactional(ModelerJpa.TRANSACTION_MANAGER)
	public FolderEntity root(ModelSource source) {
		return folderDao.findBySourceAndParentIdIsNull(source).orElseGet(() -> {
			FolderEntity root = new FolderEntity();
			root.setSource(source);
			root.setName(source.name());
			root.setCreated(Timestamp.valueOf(LocalDateTime.now()));
			log.info("Creating the {} root folder", source);
			return folderDao.save(root);
		});
	}

	/**
	 * Where a model goes when the caller names no folder: the folder the upgrade put the models
	 * of a flat installation in. Created on first use, so a client that knows nothing about
	 * folders still works.
	 */
	@Transactional(ModelerJpa.TRANSACTION_MANAGER)
	public FolderEntity defaultFolder(ModelSource source) {
		FolderEntity root = root(source);
		return folderDao.findByParentIdAndName(root.getId(), DEFAULT_FOLDER_NAME)
			.orElseGet(() -> create(root.getId(), DEFAULT_FOLDER_NAME, null));
	}

	@Transactional(value = ModelerJpa.TRANSACTION_MANAGER, readOnly = true)
	public List<FolderEntity> findAll(ModelSource source) {
		return folderDao.findBySourceOrderByNameAsc(source);
	}

	@Transactional(value = ModelerJpa.TRANSACTION_MANAGER, readOnly = true)
	public FolderEntity find(String id) {
		if (id == null || id.isBlank()) {
			throw new NoObjectFoundException("No folder id given");
		}
		return folderDao.findById(id)
			.orElseThrow(() -> new NoObjectFoundException("No folder with id " + id));
	}

	@Transactional(ModelerJpa.TRANSACTION_MANAGER)
	public FolderEntity create(String parentId, String name, String userId) {
		if (parentId == null || parentId.isBlank()) {
			throw new InvalidFolderException("parentId", "a folder is created inside another one");
		}
		FolderEntity parent = find(parentId);
		String folderName = validName(name);
		requireFreeName(parentId, folderName, null);

		FolderEntity folder = new FolderEntity();
		folder.setParentId(parent.getId());
		folder.setSource(parent.getSource());
		folder.setName(folderName);
		folder.setCreated(Timestamp.valueOf(LocalDateTime.now()));
		folder.setCreatedBy(userId);
		return folderDao.save(folder);
	}

	@Transactional(ModelerJpa.TRANSACTION_MANAGER)
	public FolderEntity rename(String id, String name, String userId) {
		FolderEntity folder = requireNotRoot(find(id), "renamed");
		String folderName = validName(name);
		requireFreeName(folder.getParentId(), folderName, id);

		folder.setName(folderName);
		return touch(folder, userId);
	}

	/** Moving keeps the id of the folder and everything below it, so links and deployments hold. */
	@Transactional(ModelerJpa.TRANSACTION_MANAGER)
	public FolderEntity move(String id, String newParentId, String userId) {
		FolderEntity folder = requireNotRoot(find(id), "moved");
		FolderEntity target = find(newParentId);

		if (target.getSource() != folder.getSource()) {
			throw new InvalidFolderException("parentId", "a folder cannot move to another source");
		}
		if (subtreeIds(id).contains(newParentId)) {
			throw new InvalidFolderException("parentId", "a folder cannot move into itself");
		}
		requireFreeName(newParentId, folder.getName(), id);

		folder.setParentId(newParentId);
		return touch(folder, userId);
	}

	@Transactional(value = ModelerJpa.TRANSACTION_MANAGER, readOnly = true)
	public FolderContents contents(String id) {
		List<String> ids = subtreeIds(id);
		return new FolderContents(
			ids.size() - 1L,
			processDiagramDao.countByFolderIdIn(ids),
			formDao.countByFolderIdIn(ids));
	}

	/**
	 * Deletes the folder with everything below it. Callers are expected to have shown what
	 * contents reports first: the models are removed for good, as deleting one from the list
	 * has always been.
	 */
	@Transactional(ModelerJpa.TRANSACTION_MANAGER)
	public FolderContents delete(String id) {
		FolderEntity folder = requireNotRoot(find(id), "deleted");
		List<String> ids = subtreeIds(id);
		FolderContents removed = contents(id);

		processDiagramDao.deleteAll(processDiagramDao.findByFolderIdIn(ids));
		formDao.deleteAll(formDao.findByFolderIdIn(ids));
		folderDao.deleteAllById(ids);

		log.info("Deleted folder {} with {} folder(s) and {} model(s)",
			folder.getName(), removed.folders(), removed.models());
		return removed;
	}

	/** The folder a model may be placed in: it has to exist, and a root holds folders only. */
	@Transactional(value = ModelerJpa.TRANSACTION_MANAGER, readOnly = true)
	public FolderEntity requireModelFolder(String folderId) {
		if (folderId == null || folderId.isBlank()) {
			throw new InvalidFolderException("folderId", "a model needs the folder it goes into");
		}
		FolderEntity folder = find(folderId);
		if (folder.getParentId() == null) {
			throw new InvalidFolderException("folderId",
				"models live in a folder below the source, not in the source itself");
		}
		return folder;
	}

	/** The folder ids of a subtree, the folder itself first. */
	private List<String> subtreeIds(String id) {
		List<String> ids = new ArrayList<>();
		Deque<String> pending = new ArrayDeque<>();
		pending.add(id);
		while (!pending.isEmpty()) {
			String current = pending.removeFirst();
			ids.add(current);
			folderDao.findByParentIdOrderByNameAsc(current).forEach(child -> pending.add(child.getId()));
		}
		return ids;
	}

	private FolderEntity touch(FolderEntity folder, String userId) {
		folder.setUpdated(Timestamp.valueOf(LocalDateTime.now()));
		folder.setUpdatedBy(userId);
		return folderDao.save(folder);
	}

	private static FolderEntity requireNotRoot(FolderEntity folder, String operation) {
		if (folder.getParentId() == null) {
			throw new InvalidFolderException("id", "the folder of a source cannot be " + operation);
		}
		return folder;
	}

	private static String validName(String name) {
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty()) {
			throw new InvalidFolderException("name", "a folder needs a name");
		}
		if (trimmed.length() > 255) {
			throw new InvalidFolderException("name", "the name is longer than 255 characters");
		}
		return trimmed;
	}

	/** Two folders with one name in one place would be indistinguishable in the tree. */
	private void requireFreeName(String parentId, String name, String allowedId) {
		Optional<FolderEntity> taken = folderDao.findByParentIdAndName(parentId, name);
		if (taken.isPresent() && !taken.get().getId().equals(allowedId)) {
			throw new InvalidFolderException("name", "this folder already holds one with that name");
		}
	}
}
