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
import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.providers.WebclientExtensions;
import org.springframework.beans.factory.DisposableBean;
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
    prefix = SevenWebclientProperties.MODELER_PREFIX,
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Slf4j
public class FolderProjectFill implements DisposableBean {

	private final FolderProvider folderProvider;
	private final WebclientExtensions webclientExtensions;

	private volatile Thread filling;

	public FolderProjectFill(FolderProvider folderProvider, WebclientExtensions webclientExtensions) {
		this.folderProvider = folderProvider;
		this.webclientExtensions = webclientExtensions;
	}

	/**
	 * Once the application is up, and in a thread of its own: in Tomcat or WildFly the event comes
	 * during the deployment, and an engine-rest in the same server answers only after it, so asking
	 * the engine here would hold the deployment forever.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void fillInBackground() {
		Thread thread = new Thread(this::fill, "cibseven-folder-projects");
		thread.setDaemon(true);
		filling = thread;
		thread.start();
	}

	@Override
	public void destroy() {
		Thread thread = filling;
		if (thread != null) {
			thread.interrupt();
		}
	}

	/**
	 * The engine creates the table on its own schedule, so a database that cannot be read yet is
	 * logged and the start goes on; the next start fills the folders. Without the webclient tables,
	 * which the engine may have switched off, there is nothing to fill.
	 */
	void fill() {
		if (webclientExtensions.switchedOffNow()) {
			log.info("The engine has no webclient tables; the modeler is off");
			return;
		}
		// Stopped while waiting for the engine: the application is shutting down
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		try {
			int filled = folderProvider.fillProjects();
			if (filled > 0) {
				log.info("Stored the project of {} modeler folder(s)", filled);
			}
		} catch (RuntimeException e) {
			// Most often a table the engine has not created, which needs no stack trace to explain
			log.warn("Could not store the project of the modeler folders; lists may not leave out "
				+ "the models of hidden projects in them until the next start: {}", e.getMessage());
			log.debug("Storing the project of the modeler folders failed", e);
		}
	}
}
