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
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cibseven.persistence.CibsevenJpa;
import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.repository.FolderRepository;
import org.cibseven.modeler.repository.FormRepository;
import org.cibseven.modeler.repository.ProcessDiagramRepository;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.exception.InvalidFolderException;
import org.cibseven.webapp.exception.NoObjectFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * The modeler folder tree: what may be created, renamed, moved and deleted, and what a folder
 * holds. A model belongs to exactly one folder, so the tree is also the path a model is reached
 * under. Only models kept in the database have folders; a repository or a directory brings its
 * own tree.
 */
@Component
@Slf4j
public class FolderProvider {

	@Autowired
	private FolderRepository folderDao;

	@Autowired
	private ProcessDiagramRepository processDiagramDao;

	@Autowired
	private FormRepository formDao;

	// Replaced by the bean of the edition; open until then, so a provider built by hand works as before
	@Autowired
	private ProjectAccess projectAccess = new OpenProjectAccess();

	/** What a folder holds, counted through the whole subtree. */
	public record FolderContents(long folders, long diagrams, long forms) {

		public long models() {
			return diagrams + forms;
		}
	}

	@Transactional(value = CibsevenJpa.TRANSACTION_MANAGER, readOnly = true)
	public List<FolderEntity> findAll() {
		return folderDao.findAllByOrderByNameAsc();
	}

	@Transactional(value = CibsevenJpa.TRANSACTION_MANAGER, readOnly = true)
	public FolderEntity find(String id) {
		if (id == null || id.isBlank()) {
			throw new NoObjectFoundException("No folder id given");
		}
		return folderDao.findById(id)
			.orElseThrow(() -> new NoObjectFoundException("No folder with id " + id));
	}

	/** Without a parent the folder is created at the top level. */
	@Transactional(CibsevenJpa.TRANSACTION_MANAGER)
	public FolderEntity create(String parentId, String name, String userId) {
		FolderEntity parent = parentId == null || parentId.isBlank() ? null : find(parentId);
		String parentFolderId = parent == null ? null : parent.getId();
		String folderName = validName(name);
		requireFreeName(parentFolderId, folderName, null);
		return store(parentFolderId, folderName, userId);
	}

	private FolderEntity store(String parentFolderId, String folderName, String userId) {
		FolderEntity folder = new FolderEntity();
		folder.setParentId(parentFolderId);
		folder.setName(folderName);
		folder.setCreated(Timestamp.valueOf(LocalDateTime.now()));
		folder.setCreatedBy(userId);
		FolderEntity created = folderDao.save(folder);
		if (parentFolderId == null) {
			projectAccess.projectCreated(userId, created);
		}
		return created;
	}

	/**
	 * The folder of the user, created the first time it is asked for: the folder at the top level
	 * the user created under their own name. A folder somebody else named after the user is not it,
	 * since it may be closed to the user; they get a folder of their own beside it. Two first visits
	 * at once can store it twice, since no key covers the top level; the oldest is the one used from
	 * then on.
	 */
	@Transactional(CibsevenJpa.TRANSACTION_MANAGER)
	public FolderEntity userFolder(String userId) {
		String name = validName(userId);
		Optional<FolderEntity> existing = folderDao.findFirstByParentIdIsNullAndNameAndCreatedByOrderByCreatedAsc(name, userId);
		// A folder from before there were members gets its owner now
		existing.ifPresent(folder -> projectAccess.projectCreated(userId, folder));
		return existing.orElseGet(() -> store(null, name, userId));
	}

	/** What a request changes about a folder: it may rename it, move it, or both at once. */
	public record FolderChange(boolean renaming, String name, boolean moving, String parentId) {
	}

	/**
	 * Applies both in one transaction, so a move the tree refuses does not leave the folder
	 * renamed. A user folder stays where and what it is unless the caller may manage user folders,
	 * which is only asked for a user folder.
	 */
	@Transactional(CibsevenJpa.TRANSACTION_MANAGER)
	public FolderEntity update(String id, FolderChange change, String userId, BooleanSupplier manageUserFolders) {
		FolderEntity folder = find(id);
		requireChangeable(folder, manageUserFolders);
		if (change.renaming()) {
			folder = rename(id, change.name(), userId);
		}
		if (change.moving()) {
			folder = move(id, change.parentId(), userId);
		}
		return folder;
	}

	@Transactional(CibsevenJpa.TRANSACTION_MANAGER)
	public FolderEntity rename(String id, String name, String userId) {
		FolderEntity folder = find(id);
		String folderName = validName(name);
		// Keeping the name changes nothing; a user folder may share it with somebody else's
		if (!folderName.equals(folder.getName())) {
			requireFreeName(folder.getParentId(), folderName, id);
		}

		folder.setName(folderName);
		return touch(folder, userId);
	}

	/** Moving keeps the id of the folder and everything below it, so links and deployments hold. */
	@Transactional(CibsevenJpa.TRANSACTION_MANAGER)
	public FolderEntity move(String id, String newParentId, String userId) {
		FolderEntity folder = find(id);
		FolderEntity target = newParentId == null || newParentId.isBlank() ? null : find(newParentId);

		if (target != null && subtreeIds(id).contains(target.getId())) {
			throw new InvalidFolderException("parentId", "a folder cannot move into itself");
		}
		String targetId = target == null ? null : target.getId();
		if (!Objects.equals(targetId, folder.getParentId())) {
			requireFreeName(targetId, folder.getName(), id);
		}

		// Taken out to the top level, the folder is a project of its own from now on
		String formerProjectId = folder.getParentId() != null && targetId == null ? projectOf(id) : null;
		folder.setParentId(targetId);
		FolderEntity moved = touch(folder, userId);
		if (formerProjectId != null) {
			projectAccess.becameProject(userId, moved, formerProjectId);
		}
		return moved;
	}

	/** The project of every folder: the top-level folder it lies under, a project being its own. */
	@Transactional(value = CibsevenJpa.TRANSACTION_MANAGER, readOnly = true)
	public Map<String, String> projectsByFolder() {
		Map<String, String> parents = new HashMap<>();
		folderDao.findAll().forEach(folder -> parents.put(folder.getId(), folder.getParentId()));
		Map<String, String> projects = new HashMap<>();
		parents.keySet().forEach(id -> projects.put(id, rootOf(id, parents)));
		return projects;
	}

	/** The project a folder belongs to, or null for a folder that does not exist. */
	@Transactional(value = CibsevenJpa.TRANSACTION_MANAGER, readOnly = true)
	public String projectOf(String folderId) {
		return folderId == null ? null : projectsByFolder().get(folderId);
	}

	private static String rootOf(String id, Map<String, String> parents) {
		String current = id;
		// Bounded by the number of folders, so a broken parent chain cannot loop for ever
		for (int depth = 0; depth < parents.size(); depth++) {
			String parent = parents.get(current);
			if (parent == null || !parents.containsKey(parent)) {
				return current;
			}
			current = parent;
		}
		return current;
	}

	@Transactional(value = CibsevenJpa.TRANSACTION_MANAGER, readOnly = true)
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
	 * has always been. A user folder is only deleted for a caller who may manage user folders.
	 */
	@Transactional(CibsevenJpa.TRANSACTION_MANAGER)
	public FolderContents delete(String id, BooleanSupplier manageUserFolders) {
		FolderEntity folder = find(id);
		requireChangeable(folder, manageUserFolders);
		List<String> ids = subtreeIds(id);
		FolderContents removed = contents(id);

		processDiagramDao.deleteAll(processDiagramDao.findByFolderIdIn(ids));
		formDao.deleteAll(formDao.findByFolderIdIn(ids));
		// Deepest first: a folder points at its parent, so a parent removed before its children
		// leaves the database refusing the delete
		List<String> childrenFirst = new ArrayList<>(ids);
		Collections.reverse(childrenFirst);
		folderDao.deleteAllById(childrenFirst);

		log.info("Deleted folder {} with {} folder(s) and {} model(s)",
			folder.getName(), removed.folders(), removed.models());
		return removed;
	}

	/** The folder a model may be placed in: it has to be named, and it has to exist. */
	@Transactional(value = CibsevenJpa.TRANSACTION_MANAGER, readOnly = true)
	public FolderEntity requireModelFolder(String folderId) {
		if (folderId == null || folderId.isBlank()) {
			throw new InvalidFolderException("folderId", "a model needs the folder it goes into");
		}
		return find(folderId);
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

	/** Renamed or moved, a user folder would stop being one, and the next visit would create another. */
	private static void requireChangeable(FolderEntity folder, BooleanSupplier manageUserFolders) {
		if (folder.isUserFolder() && !manageUserFolders.getAsBoolean()) {
			throw new AccessDeniedException("The folder of user " + folder.getName()
				+ " can only be renamed, moved or deleted by an administrator");
		}
	}

	private FolderEntity touch(FolderEntity folder, String userId) {
		folder.setUpdated(Timestamp.valueOf(LocalDateTime.now()));
		folder.setUpdatedBy(userId);
		return folderDao.save(folder);
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

	/**
	 * Two folders with one name in one place would be indistinguishable in the tree. The database
	 * cannot be left to decide it at the top level: a unique key over a null parent is a no-op on
	 * most databases and too strict on the rest.
	 */
	private void requireFreeName(String parentId, String name, String allowedId) {
		Optional<FolderEntity> taken = parentId == null
			? folderDao.findFirstByParentIdIsNullAndNameOrderByCreatedAsc(name)
			: folderDao.findByParentIdAndName(parentId, name);
		if (taken.isPresent() && !taken.get().getId().equals(allowedId)) {
			throw new InvalidFolderException("name", "a folder with that name is already there");
		}
	}
}
