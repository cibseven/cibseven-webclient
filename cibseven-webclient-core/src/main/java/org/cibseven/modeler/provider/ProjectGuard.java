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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.modeler.provider.ProjectAccess.Action;
import org.cibseven.modeler.provider.ProjectAccess.ProjectRights;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.AccessDeniedException;
import org.cibseven.webapp.exception.InvalidFolderException;

/**
 * Asks {@link ProjectAccess} on behalf of the modeler endpoints, which know a folder or the folder
 * of a model but not the project it lies in. With every project open it asks nothing.
 */
@Component
public class ProjectGuard {

	/** Lets everything through without looking anything up, for an endpoint built by hand. */
	public static final ProjectGuard OPEN = new ProjectGuard(new OpenProjectAccess(), null);

	/** What the modeler may offer the caller, for the start page to show only that. */
	public record AccessOverview(Map<String, ProjectRights> projects, boolean movesBetweenProjects) {
	}

	private final ProjectAccess access;
	private final FolderProvider folderProvider;

	public ProjectGuard(ProjectAccess access, FolderProvider folderProvider) {
		this.access = access;
		this.folderProvider = folderProvider;
	}

	/**
	 * @throws AccessDeniedException when the user may not do this in the project of the folder. A
	 *         model from before there were folders has none, and is left to modeler access alone
	 */
	private static final String PROJECTS = ProjectGuard.class.getName() + ".projects";

	/**
	 * The project of every folder, read once per request: a request asks about several folders,
	 * and each answer would otherwise read the whole tree again.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, String> projects() {
		RequestAttributes request = RequestContextHolder.getRequestAttributes();
		if (request != null && request.getAttribute(PROJECTS, RequestAttributes.SCOPE_REQUEST) instanceof Map<?, ?> kept) {
			return (Map<String, String>) kept;
		}
		Map<String, String> projects = folderProvider.projectsByFolder();
		if (request != null) {
			request.setAttribute(PROJECTS, projects, RequestAttributes.SCOPE_REQUEST);
		}
		return projects;
	}

	public void require(CIBUser user, String folderId, Action action) {
		if (access.isOpen() || folderId == null) {
			return;
		}
		access.require(user, project(folderId), action);
	}

	/** Renaming or deleting a whole project is up to its admins; any other folder, to its editors. */
	public void requireFolderChange(CIBUser user, FolderEntity folder) {
		if (access.isOpen()) {
			return;
		}
		require(user, folder.getId(), folder.getParentId() == null ? Action.MANAGE : Action.EDIT);
	}

	/**
	 * A folder or a model leaves one folder for another. Taking a folder out to the top level makes
	 * it a project with the members it had, so only the project it leaves is asked. Moving into
	 * another project would change who sees it on both sides, which the edition may refuse.
	 *
	 * @param fromFolderId the folder moved, or the folder a moved model is in
	 * @param toFolderId the new parent, or the new folder of a model; null for the top level
	 */
	public void requireMove(CIBUser user, String fromFolderId, String toFolderId) {
		if (access.isOpen()) {
			return;
		}
		require(user, fromFolderId, Action.EDIT);
		if (toFolderId == null) {
			return;
		}
		require(user, toFolderId, Action.EDIT);
		if (!access.movesBetweenProjects() && fromFolderId != null
				&& !Objects.equals(project(fromFolderId), project(toFolderId))) {
			throw new InvalidFolderException("folderId", "only a copy may go into another project");
		}
	}

	/**
	 * The folder ids for a NOT IN clause, which may not be empty. A folder id is a UUID, so the
	 * stand-in matches none; it is not blank either, which Oracle would read as null.
	 */
	public static Collection<String> notEmpty(Collection<String> folderIds) {
		return folderIds == null || folderIds.isEmpty() ? List.of("-") : folderIds;
	}

	/**
	 * The most hidden folders a query is given to leave out. Oracle takes at most 1000 values in
	 * a list and SQL Server 2100 parameters in a query, so this stays well below both.
	 */
	static final int QUERY_LIMIT = 500;

	private static final int BATCH = 200;

	/** One page of a list query, leaving out the models in the folders given. */
	@FunctionalInterface
	public interface PagedQuery<T> {
		List<T> fetch(Collection<String> hiddenFolderIds, int page, int size);
	}

	/**
	 * A page of what the user may see. A short list of hidden folders goes into the query. A longer
	 * one would break it, so the rows are read in batches instead and the hidden ones left out
	 * here, which keeps the page exactly what the query would have returned.
	 */
	public static <T> List<T> visiblePage(Collection<String> hiddenFolderIds, int firstResult, int maxResults,
			PagedQuery<T> query, Function<T, String> folderOf) {
		if (hiddenFolderIds == null || hiddenFolderIds.size() <= QUERY_LIMIT) {
			return query.fetch(notEmpty(hiddenFolderIds), firstResult / maxResults, maxResults);
		}
		Set<String> hidden = new HashSet<>(hiddenFolderIds);
		List<T> page = new ArrayList<>();
		int skipped = 0;
		for (int batch = 0; ; batch++) {
			List<T> rows = query.fetch(notEmpty(List.of()), batch, BATCH);
			for (T row : rows) {
				String folderId = folderOf.apply(row);
				if (folderId != null && hidden.contains(folderId)) {
					continue;
				}
				if (skipped < firstResult) {
					skipped++;
					continue;
				}
				page.add(row);
				if (page.size() == maxResults) {
					return page;
				}
			}
			if (rows.size() < BATCH) {
				return page;
			}
		}
	}

	/** The folders the user may not see: every folder of a project hidden from them. */
	public Set<String> hiddenFolderIds(CIBUser user) {
		if (access.isOpen()) {
			return Set.of();
		}
		Map<String, String> projects = projects();
		Set<String> hiddenProjects = access.hidden(user, new HashSet<>(projects.values()));
		Set<String> hidden = new HashSet<>();
		projects.forEach((folderId, projectId) -> {
			if (hiddenProjects.contains(projectId)) {
				hidden.add(folderId);
			}
		});
		return hidden;
	}

	/** The folders the user may see, in the order given. */
	public List<FolderEntity> visible(CIBUser user, List<FolderEntity> folders) {
		if (access.isOpen()) {
			return folders;
		}
		Set<String> hidden = hiddenFolderIds(user);
		return folders.stream().filter(folder -> !hidden.contains(folder.getId())).toList();
	}

	public AccessOverview overview(CIBUser user) {
		if (access.isOpen()) {
			return new AccessOverview(Map.of(), access.movesBetweenProjects());
		}
		Set<String> projectIds = new HashSet<>(projects().values());
		projectIds.removeAll(access.hidden(user, projectIds));
		return new AccessOverview(access.rights(user, projectIds), access.movesBetweenProjects());
	}

	/** A folder deleted meanwhile has no project; asking about the folder itself refuses it then. */
	private String project(String folderId) {
		return projects().getOrDefault(folderId, folderId);
	}
}
