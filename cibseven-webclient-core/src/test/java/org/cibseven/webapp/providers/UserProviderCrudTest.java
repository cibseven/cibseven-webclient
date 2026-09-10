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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.rest.model.Credentials;
import org.cibseven.webapp.rest.model.NewUser;
import org.cibseven.webapp.rest.model.SevenUser;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.cibseven.webapp.rest.model.User;
import org.cibseven.webapp.rest.model.UserGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Covers the user, group and authorization endpoints of {@link UserProvider}.
 * The permission checks are covered by {@link UserProviderIT}.
 */
public class UserProviderCrudTest {

	private MockEngineRest engine;
	private UserProvider userProvider;
	private CIBUser user;

	@BeforeEach
	void setUp() throws Exception {
		engine = new MockEngineRest();
		userProvider = engine.wire(new UserProvider());
		// UserProvider adds two @Value fields of its own on top of SevenProviderBase's
		ReflectionTestUtils.setField(userProvider, "userProvider", "org.cibseven.webapp.auth.SevenUserProvider");
		ReflectionTestUtils.setField(userProvider, "wildcard", "");
		user = MockEngineRest.user();
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.close();
	}

	// ---------- users ----------

	@Test
	void fetchUsers_mapsTheEngineUsers() throws Exception {
		engine.enqueueJson("[{\"id\":\"demo\",\"firstName\":\"Demo\",\"lastName\":\"User\"}]");

		Collection<SevenUser> users = userProvider.fetchUsers(user);

		assertThat(users).hasSize(1);
		assertThat(users.iterator().next().getId()).isEqualTo("demo");
		assertThat(engine.takePath()).isEqualTo("/user");
	}

	@Test
	void countUsers_readsTheCountField() throws Exception {
		engine.enqueueJson("{\"count\":11}");

		assertThat(userProvider.countUsers(new HashMap<>(), user)).isEqualTo(11L);
		assertThat(engine.takePath()).isEqualTo("/user/count");
	}

	@Test
	void countUsers_toleratesANullFilterMap() throws Exception {
		engine.enqueueJson("{\"count\":2}");

		assertThat(userProvider.countUsers(null, user)).isEqualTo(2L);
	}

	@Test
	void countUsers_returnsZeroWhenTheEngineSendsNoBody() throws Exception {
		engine.enqueueEmpty(204);

		assertThat(userProvider.countUsers(new HashMap<>(), user)).isZero();
	}

	@Test
	void getUserProfile_asksForTheProfileSubresource() throws Exception {
		engine.enqueueJson("{\"id\":\"demo\",\"firstName\":\"Demo\"}");

		SevenUser profile = userProvider.getUserProfile("demo", user);

		assertThat(profile.getFirstName()).isEqualTo("Demo");
		assertThat(engine.takePath()).isEqualTo("/user/demo/profile");
	}

	@Test
	void createUser_postsProfileAndCredentialsTogether() throws Exception {
		engine.enqueueJson("");
		NewUser request = new NewUser(new User("demo", "Demo", "User", "demo@example.com"),
			new Credentials("s3cret"));

		userProvider.createUser(request, user);

		var httpRequest = engine.take();
		assertThat(httpRequest.getMethod()).isEqualTo("POST");
		assertThat(httpRequest.getPath()).endsWith("/user/create");
		String body = httpRequest.getBody().readUtf8();
		assertThat(body).contains("\"profile\":").contains("\"credentials\":");
		assertThat(body).contains("\"id\":\"demo\"").contains("\"password\":\"s3cret\"");
	}

	@Test
	void updateUserProfile_putsTheProfile() throws Exception {
		engine.enqueueEmpty(204);

		userProvider.updateUserProfile("demo", new User("demo", "New", "Name", "new@example.com"), user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getPath()).endsWith("/user/demo/profile");
		assertThat(request.getBody().readUtf8()).contains("\"firstName\":\"New\"");
	}

	@Test
	void updateUserCredentials_putsToTheCredentialsSubresource() throws Exception {
		engine.enqueueEmpty(204);
		Map<String, Object> data = new HashMap<>();
		data.put("password", "newPassword");

		userProvider.updateUserCredentials("demo", data, user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getPath()).endsWith("/user/demo/credentials");
	}

	@Test
	void deleteUser_deletesTheUser() throws Exception {
		engine.enqueueEmpty(204);

		userProvider.deleteUser("demo", user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("DELETE");
		assertThat(request.getPath()).endsWith("/user/demo");
	}

	@Test
	void verifyUser_postsOnlyTheCredentialsThatWereGiven() throws Exception {
		engine.enqueueJson("{\"authenticatedUser\":\"demo\",\"authenticated\":true}");

		SevenVerifyUser verified = userProvider.verifyUser(new StandardLogin("demo", "s3cret"), user);

		assertThat(verified.getAuthenticatedUser()).isEqualTo("demo");
		var request = engine.take();
		assertThat(request.getPath()).endsWith("/identity/verify");
		assertThat(request.getBody().readUtf8()).isEqualTo("{\"username\":\"demo\",\"password\":\"s3cret\"}");
	}

	@Test
	void verifyUser_omitsAMissingPassword() throws Exception {
		engine.enqueueJson("{\"authenticated\":false}");

		userProvider.verifyUser(new StandardLogin("demo", null), user);

		// a null field is left out rather than sent as JSON null
		assertThat(engine.take().getBody().readUtf8()).isEqualTo("{\"username\":\"demo\"}");
	}

	// ---------- groups and memberships ----------

	@Test
	void findGroups_asksTheGroupEndpoint() throws Exception {
		engine.enqueueJson("[{\"id\":\"sales\",\"name\":\"Sales\",\"type\":\"WORKFLOW\"}]");

		Collection<UserGroup> groups = userProvider.findGroups(
			Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
			Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), user);

		assertThat(groups).hasSize(1);
		assertThat(groups.iterator().next().getName()).isEqualTo("Sales");
		assertThat(engine.takePath()).startsWith("/group");
	}

	@Test
	void createGroup_postsToTheGroupCreateEndpoint() throws Exception {
		engine.enqueueJson("");

		userProvider.createGroup(new UserGroup("sales", "Sales", "WORKFLOW"), user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).endsWith("/group/create");
		assertThat(request.getBody().readUtf8()).contains("\"id\":\"sales\"");
	}

	@Test
	void updateGroup_putsToTheGroupById() throws Exception {
		engine.enqueueEmpty(204);

		userProvider.updateGroup("sales", new UserGroup("sales", "Sales renamed", "WORKFLOW"), user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getPath()).endsWith("/group/sales");
	}

	@Test
	void deleteGroup_deletesTheGroup() throws Exception {
		engine.enqueueEmpty(204);

		userProvider.deleteGroup("sales", user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("DELETE");
		assertThat(request.getPath()).endsWith("/group/sales");
	}

	@Test
	void addMemberToGroup_putsTheMembership() throws Exception {
		engine.enqueueEmpty(204);

		userProvider.addMemberToGroup("sales", "demo", user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getPath()).endsWith("/group/sales/members/demo");
	}

	@Test
	void deleteMemberFromGroup_deletesTheMembership() throws Exception {
		engine.enqueueEmpty(204);

		userProvider.deleteMemberFromGroup("sales", "demo", user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("DELETE");
		assertThat(request.getPath()).endsWith("/group/sales/members/demo");
	}

	// ---------- authorizations ----------

	@Test
	void findAuthorization_asksTheAuthorizationEndpoint() throws Exception {
		engine.enqueueJson("[{\"id\":\"auth-1\",\"type\":1,\"resourceType\":6,\"resourceId\":\"*\"}]");

		var authorizations = userProvider.findAuthorization(
			Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
			Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), user);

		assertThat(authorizations).hasSize(1);
		assertThat(engine.takePath()).startsWith("/authorization");
	}

	@Test
	void updateAuthorization_putsToTheAuthorizationById() throws Exception {
		engine.enqueueEmpty(204);

		userProvider.updateAuthorization("auth-1", new HashMap<>(), user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getPath()).endsWith("/authorization/auth-1");
	}

	@Test
	void deleteAuthorization_deletesTheAuthorization() throws Exception {
		engine.enqueueEmpty(204);

		userProvider.deleteAuthorization("auth-1", user);

		var request = engine.take();
		assertThat(request.getMethod()).isEqualTo("DELETE");
		assertThat(request.getPath()).endsWith("/authorization/auth-1");
	}
}
