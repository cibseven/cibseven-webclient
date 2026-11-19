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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.cibseven.modeler.repository.DiagramUsageRepository;
import org.cibseven.modeler.repository.UserSessionRepository;
import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.providers.WebclientExtensions;
import org.junit.jupiter.api.Test;

class DiagramUsageProviderCleanupTest {

	private final SevenWebclientProperties properties = new SevenWebclientProperties();
	private final UserSessionRepository sessions = mock(UserSessionRepository.class);
	private final WebclientExtensions extensions = mock(WebclientExtensions.class);
	private final DiagramUsageProvider provider =
		new DiagramUsageProvider(properties, mock(DiagramUsageRepository.class), sessions, extensions);

	@Test
	void removesOldSessions() {
		when(extensions.switchedOff()).thenReturn(false);

		provider.removeOldVersions();

		verify(sessions).removeOldSessions(properties.getModeler().getDeleteUsages().getUsageLimit());
	}

	/** The tables are not there, so the scheduled cleanup would fail on every run. */
	@Test
	void leavesTablesTheEngineHasSwitchedOffAlone() {
		when(extensions.switchedOff()).thenReturn(true);

		provider.removeOldVersions();

		verify(sessions, never()).removeOldSessions(anyInt());
	}
}
