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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.AccessDeniedException;

/**
 * Who may do what in a modeler project: a folder at the top level, with everything below it. The
 * modeler endpoints ask before they read or change a folder or a model. {@link OpenProjectAccess}
 * lets everyone with modeler access do everything; an edition with project members replaces it
 * with a bean of its own.
 */
public interface ProjectAccess {

	/** What a request does in a project. Deploying a model counts as editing it. */
	enum Action { VIEW, EDIT, MANAGE }

	/**
	 * What the caller may do in one project, so the modeler only offers that. A project is
	 * restricted once it has members; the role is the caller's own there, null without one.
	 */
	record ProjectRights(String role, boolean restricted, boolean edit, boolean manageMembers) {

		public static final ProjectRights OPEN = new ProjectRights(null, false, true, false);
	}

	/** Nothing is restricted, so callers need not resolve the project of anything. */
	default boolean isOpen() {
		return false;
	}

	/** @throws AccessDeniedException when the user may not do this in the project */
	void require(CIBUser user, String projectId, Action action);

	/** The projects among these that the user may not see at all. */
	Set<String> hidden(CIBUser user, Collection<String> projectIds);

	/** The user's rights in each of these projects. */
	Map<String, ProjectRights> rights(CIBUser user, Collection<String> projectIds);

	/** Whether a folder or a model may move from one project into another. */
	boolean movesBetweenProjects();

	/**
	 * A project came into being, or a user opened their own folder: whoever did is its first
	 * admin. Asked again for a project that has members, nothing changes.
	 */
	void projectCreated(String userId, FolderEntity project);

	/** A folder moved to the top level and is a project now, with the members of the one it left. */
	void becameProject(String userId, FolderEntity folder, String formerProjectId);
}
