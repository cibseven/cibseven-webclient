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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FolderProjectFillTest {

	private final FolderProvider folders = Mockito.mock(FolderProvider.class);
	private final FolderProjectFill fill = new FolderProjectFill(folders);

	@Test
	void storesTheProjectsOfTheFoldersOnStart() {
		when(folders.fillProjects()).thenReturn(3).thenReturn(0);

		fill.fill();
		fill.fill();

		verify(folders, Mockito.times(2)).fillProjects();
	}

	/** The engine may not have created the table yet; the webclient starts all the same. */
	@Test
	void letsTheStartGoOnWhenTheFoldersCannotBeRead() {
		when(folders.fillProjects()).thenThrow(new IllegalStateException("MOD_FOLDERS not found"));

		assertThatCode(fill::fill).doesNotThrowAnyException();
	}
}
