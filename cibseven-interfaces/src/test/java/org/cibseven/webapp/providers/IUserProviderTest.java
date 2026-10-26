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
package org.cibseven.webapp.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.UserGroup;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

/** The groups of a user, as a provider that cannot ask the engine directly finds them. */
class IUserProviderTest {

	@Test
	void asksForTheGroupsTheUserIsAMemberOf() {
		IUserProvider provider = mock(IUserProvider.class, withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
		CIBUser user = new CIBUser("demo");
		when(provider.findGroups(any(), any(), any(), any(), eq(Optional.of("demo")), any(), any(), any(), any(), any(), eq(user)))
			.thenReturn(List.of(new UserGroup("sales", "Sales", null), new UserGroup("claims", "Claims", null)));

		assertThat(provider.findGroupIdsOfUser(user)).containsExactly("sales", "claims");
	}
}
