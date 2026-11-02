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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import org.cibseven.modeler.model.FolderEntity;
import org.cibseven.webapp.auth.CIBUser;

/** Every project is open to everyone with modeler access, and folders move wherever they are put. */
@Component
public class OpenProjectAccess implements ProjectAccess {

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public void require(CIBUser user, String projectId, Action action) {
		// Modeler access, checked by every endpoint already, is all it takes
	}

	@Override
	public Set<String> hidden(CIBUser user, Collection<String> projectIds) {
		return Set.of();
	}

	@Override
	public Map<String, ProjectRights> rights(CIBUser user, Collection<String> projectIds) {
		return projectIds.stream().distinct().collect(Collectors.toMap(Function.identity(), id -> ProjectRights.OPEN));
	}

	@Override
	public boolean movesBetweenProjects() {
		return true;
	}

	@Override
	public void projectCreated(String userId, FolderEntity project) {
		// Nobody is a member of anything
	}

	@Override
	public void becameProject(String userId, FolderEntity folder, String formerProjectId) {
		// There are no members to take along
	}
}
