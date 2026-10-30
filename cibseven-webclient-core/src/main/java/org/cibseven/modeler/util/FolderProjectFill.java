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
package org.cibseven.modeler.util;

import org.cibseven.modeler.provider.FolderProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Stores the project of the folders that have none yet, such as those from before it was kept on
 * them: the lists leave out a hidden project through it, and would miss their models otherwise.
 */
@Component
@ConditionalOnProperty(
    prefix = "cibseven.webclient.modeler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Slf4j
public class FolderProjectFill {

	private final FolderProvider folderProvider;

	public FolderProjectFill(FolderProvider folderProvider) {
		this.folderProvider = folderProvider;
	}

	/**
	 * Once the application is up. The engine creates the table on its own schedule, so a database
	 * that cannot be read yet is logged and the start goes on; the next start fills the folders.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void fill() {
		try {
			int filled = folderProvider.fillProjects();
			if (filled > 0) {
				log.info("Stored the project of {} modeler folder(s)", filled);
			}
		} catch (RuntimeException e) {
			log.warn("Could not store the project of the modeler folders; lists may not leave out "
				+ "the models of hidden projects in them until the next start", e);
		}
	}
}
