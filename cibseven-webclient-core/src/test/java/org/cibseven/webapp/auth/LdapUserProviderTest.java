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
package org.cibseven.webapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Base64;
import java.util.List;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.exception.SystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Covers everything in {@link LdapUserProvider} that does not require a directory server: the
 * token round-trip, the user-info contract, and the failure translation when the directory cannot
 * be reached.
 * <p>
 * The authenticated search paths ({@code login}, {@code getFullUserDN},
 * {@code verify(Claims, Date)}) build their {@code InitialDirContext} inline from a Hashtable, so
 * there is no seam to substitute and covering them would mean adding an in-process LDAP server
 * (for example unboundid-ldapsdk) as a new test dependency. That is deliberately not done here;
 * the unreachable-server tests below at least pin how those paths report failure.
 */
public class LdapUserProviderTest {

	private static final String SECRET = Base64.getEncoder().encodeToString(new byte[64]);

	private LdapUserProvider provider;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() {
		provider = new LdapUserProvider();
		// a port nothing listens on, so every directory call fails the same way
		ReflectionTestUtils.setField(provider, "serverURL", "ldap://127.0.0.1:1");
		ReflectionTestUtils.setField(provider, "ldapUser", "cn=admin");
		ReflectionTestUtils.setField(provider, "ldapPassword", "secret");
		ReflectionTestUtils.setField(provider, "ldapFolder", "ou=people,dc=example,dc=com");
		ReflectionTestUtils.setField(provider, "ldapNameAttribute", "uid");
		ReflectionTestUtils.setField(provider, "ldapDisplayNameAttribute", "cn");
		ReflectionTestUtils.setField(provider, "ldapUserClass", "person");
		ReflectionTestUtils.setField(provider, "ldapAttributesFilters", List.of("samAccountName", "name"));
		ReflectionTestUtils.setField(provider, "ldapModifiedDateFormat", "yyyyMMddHHmmss'Z'");
		ReflectionTestUtils.setField(provider, "ldapCountLimit", 400);
		ReflectionTestUtils.setField(provider, "ldapFollowReferrals", "follow");
		ReflectionTestUtils.setField(provider, "secret", SECRET);
		ReflectionTestUtils.setField(provider, "validMinutes", 60L);
		ReflectionTestUtils.setField(provider, "prolongMinutes", 30L);
		provider.init();

		request = mock(HttpServletRequest.class);
	}

	private static String bare(String authToken) {
		return authToken.startsWith("Bearer ") ? authToken.substring("Bearer ".length()) : authToken;
	}

	// ---------- configuration ----------

	@Test
	void init_buildsTheTokenSettingsFromTheConfiguredSecret() {
		assertThat(provider.getSettings().getSecret()).isEqualTo(SECRET);
		assertThat(provider.getSettings().getValid()).isEqualTo(java.time.Duration.ofMinutes(60));
	}

	// ---------- failure translation ----------

	@Test
	void login_reportsAnUnreachableDirectoryAsATechnicalFailure() {
		// a directory outage is not a bad password, and the user must not be told it is
		assertThatThrownBy(() -> provider.login(new StandardLogin("demo", "demo"), request))
			.isInstanceOf(SystemException.class)
			.hasMessageContaining("Login failed due to technical error");
	}

	@Test
	void verifyWithIssuedAt_reportsAnUnreachableDirectoryAsATechnicalFailure() {
		io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);

		assertThatThrownBy(() -> provider.verify(claims, new java.util.Date()))
			.isInstanceOf(SystemException.class);
	}

	// ---------- user info ----------

	@Test
	void getUserInfo_returnsTheSameUserForItsOwnId() {
		CIBUser user = new CIBUser("demo");

		assertThat(provider.getUserInfo(user, "demo")).isSameAs(user);
	}

	@Test
	void getUserInfo_refusesAnotherUsersInfo() {
		assertThatThrownBy(() -> provider.getUserInfo(new CIBUser("demo"), "someone-else"))
			.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void getSelfInfoJSessionId_isNotSupportedByThisProvider() {
		assertThat(provider.getSelfInfoJSessionId("demo", "session", request)).isNull();
	}

	@Test
	void logout_isANoOp() {
		provider.logout(new CIBUser("demo"));
	}

	@Test
	void createLoginParams_producesAStandardLogin() {
		assertThat(provider.createLoginParams()).isInstanceOf(StandardLogin.class);
	}

	@Test
	void verifyFromClaimsAloneIsAlwaysNull() {
		// the single-argument overload is unused by this provider; the issuedAt one does the work
		assertThat(provider.verify((io.jsonwebtoken.Claims) null)).isNull();
	}

	// ---------- serialization ----------

	@Test
	void serializeThenDeserialize_roundTripsTheUser() {
		CIBUser user = new CIBUser("demo");
		user.setDisplayName("Demo User");

		CIBUser restored = (CIBUser) provider.deserialize(provider.serialize(user), "Bearer t");

		assertThat(restored.getUserID()).isEqualTo("demo");
		assertThat(restored.getDisplayName()).isEqualTo("Demo User");
		assertThat(restored.getAuthToken()).isEqualTo("Bearer t");
	}

	@Test
	void deserialize_wrapsUnreadableJsonInSystemException() {
		assertThatThrownBy(() -> provider.deserialize("not json", null))
			.isInstanceOf(SystemException.class);
	}

	// ---------- token parsing ----------

	@Test
	void parse_readsBackAUserFromAValidToken() {
		String token = io.jsonwebtoken.Jwts.builder()
			.claim("user", provider.serialize(new CIBUser("demo")))
			.claim("verify", Boolean.FALSE)
			.claim("prolongable", Boolean.FALSE)
			.expiration(new java.util.Date(System.currentTimeMillis() + 600_000))
			.signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
			.compact();

		CIBUser parsed = (CIBUser) provider.parse(token, provider.getSettings());

		assertThat(parsed.getUserID()).isEqualTo("demo");
		assertThat(parsed.getAuthToken()).isEqualTo("Bearer " + token);
	}

	@Test
	void parse_rejectsSomethingThatIsNotAToken() {
		assertThatThrownBy(() -> provider.parse("not-a-token", provider.getSettings()))
			.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void parse_reportsAnExpiredNonProlongableTokenAsExpired() {
		String token = io.jsonwebtoken.Jwts.builder()
			.claim("user", provider.serialize(new CIBUser("demo")))
			.claim("verify", Boolean.FALSE)
			.claim("prolongable", Boolean.FALSE)
			.expiration(new java.util.Date(System.currentTimeMillis() - 300_000))
			.signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
			.compact();

		assertThatThrownBy(() -> provider.parse(token, provider.getSettings()))
			.isInstanceOf(TokenExpiredException.class);
	}

	@Test
	void parse_rejectsATokenSignedWithAnotherSecret() {
		String otherSecret = Base64.getEncoder()
			.encodeToString("another-deployment-secret-long-enough-for-hs256-aaaaaaaaaaaaaa".getBytes());
		String token = io.jsonwebtoken.Jwts.builder()
			.claim("user", provider.serialize(new CIBUser("demo")))
			.claim("verify", Boolean.FALSE)
			.claim("prolongable", Boolean.FALSE)
			.expiration(new java.util.Date(System.currentTimeMillis() + 600_000))
			.signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getDecoder().decode(otherSecret)))
			.compact();

		assertThatThrownBy(() -> provider.parse(token, provider.getSettings()))
			.isInstanceOf(AuthenticationException.class);
	}
}
