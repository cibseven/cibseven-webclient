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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Date;
import java.util.Deque;
import java.util.Hashtable;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.LoginException;
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
 * {@code verify(Claims, Date)}) open their directory context through {@code openContext}, which
 * the "directory resources" tests replace with mocks to check that every context and search
 * result is closed (CIB7-2211). The unreachable-server tests pin how those paths report failure
 * against a real, unreachable directory.
 */
public class LdapUserProviderTest {

	private static final String SECRET = Base64.getEncoder().encodeToString(new byte[64]);

	private LdapUserProvider provider;
	private HttpServletRequest request;

	/** Answers every {@code openContext} call with the next prepared context. */
	private static class StubDirectoryProvider extends LdapUserProvider {

		private final Deque<DirContext> contexts = new ArrayDeque<>();

		@Override
		DirContext openContext(Hashtable<String, String> environment) {
			return contexts.pop();
		}
	}

	@BeforeEach
	void setUp() {
		provider = configure(new LdapUserProvider());
		request = mock(HttpServletRequest.class);
	}

	private static <P extends LdapUserProvider> P configure(P provider) {
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
		return provider;
	}

	private static SearchResult entry(String dn, String... attributePairs) {
		BasicAttributes attributes = new BasicAttributes(true);
		for (int i = 0; i < attributePairs.length; i += 2) {
			attributes.put(attributePairs[i], attributePairs[i + 1]);
		}
		SearchResult result = new SearchResult(dn, null, attributes);
		result.setNameInNamespace(dn);
		return result;
	}

	@SuppressWarnings("unchecked")
	private static NamingEnumeration<SearchResult> results(SearchResult... entries) throws NamingException {
		NamingEnumeration<SearchResult> results = mock(NamingEnumeration.class);
		Deque<SearchResult> remaining = new ArrayDeque<>(List.of(entries));
		when(results.hasMore()).thenAnswer(invocation -> !remaining.isEmpty());
		when(results.next()).thenAnswer(invocation -> remaining.pop());
		return results;
	}

	private static DirContext contextReturning(NamingEnumeration<SearchResult> results) throws NamingException {
		DirContext context = mock(DirContext.class);
		when(context.search(anyString(), anyString(), any(SearchControls.class))).thenReturn(results);
		return context;
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

	// ---------- directory resources (CIB7-2211) ----------

	@Test
	void login_closesBothContextsAndTheirSearchResults() throws NamingException {
		StubDirectoryProvider stub = configure(new StubDirectoryProvider());
		String dn = "uid=demo,ou=people,dc=example,dc=com";
		NamingEnumeration<SearchResult> dnResults = results(entry(dn));
		NamingEnumeration<SearchResult> userResults = results(entry(dn, "uid", "demo", "cn", "Demo User"));
		DirContext serviceContext = contextReturning(dnResults);
		DirContext userContext = contextReturning(userResults);
		stub.contexts.add(serviceContext);
		stub.contexts.add(userContext);

		CIBUser user = stub.login(new StandardLogin("demo", "secret"), request);

		assertThat(user.getUserID()).isEqualTo("demo");
		assertThat(user.getDisplayName()).isEqualTo("Demo User");
		verify(dnResults).close();
		verify(serviceContext).close();
		verify(userResults).close();
		verify(userContext).close();
	}

	@Test
	void login_closesTheServiceContextWhenTheUserIsUnknown() throws NamingException {
		StubDirectoryProvider stub = configure(new StubDirectoryProvider());
		NamingEnumeration<SearchResult> noResults = results();
		DirContext serviceContext = contextReturning(noResults);
		stub.contexts.add(serviceContext);

		assertThatThrownBy(() -> stub.login(new StandardLogin("nobody", "secret"), request))
			.isInstanceOf(LoginException.class);
		verify(noResults).close();
		verify(serviceContext).close();
	}

	@Test
	void login_closesTheUserContextWhenTheUserEntryDoesNotMatch() throws NamingException {
		StubDirectoryProvider stub = configure(new StubDirectoryProvider());
		String dn = "uid=demo,ou=people,dc=example,dc=com";
		DirContext serviceContext = contextReturning(results(entry(dn)));
		NamingEnumeration<SearchResult> noUser = results();
		DirContext userContext = contextReturning(noUser);
		stub.contexts.add(serviceContext);
		stub.contexts.add(userContext);

		assertThatThrownBy(() -> stub.login(new StandardLogin("demo", "secret"), request))
			.isInstanceOf(LoginException.class);
		verify(serviceContext).close();
		verify(noUser).close();
		verify(userContext).close();
	}

	@Test
	void login_closesTheContextWhenTheSearchFails() throws NamingException {
		StubDirectoryProvider stub = configure(new StubDirectoryProvider());
		DirContext serviceContext = mock(DirContext.class);
		when(serviceContext.search(anyString(), anyString(), any(SearchControls.class)))
			.thenThrow(new NamingException("directory unavailable"));
		stub.contexts.add(serviceContext);

		assertThatThrownBy(() -> stub.login(new StandardLogin("demo", "secret"), request))
			.isInstanceOf(SystemException.class);
		verify(serviceContext).close();
	}

	@Test
	void verifyWithIssuedAt_closesTheContextAndItsSearchResults() throws NamingException {
		StubDirectoryProvider stub = configure(new StubDirectoryProvider());
		NamingEnumeration<SearchResult> userResults = results(entry("uid=demo,ou=people,dc=example,dc=com",
				"modifyTimestamp", "20200101000000Z", "cn", "Demo User"));
		DirContext context = contextReturning(userResults);
		stub.contexts.add(context);
		io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
		when(claims.getSubject()).thenReturn("demo");
		when(claims.get("user")).thenReturn(stub.serialize(new CIBUser("demo")));

		CIBUser user = (CIBUser) stub.verify(claims, new Date());

		assertThat(user.getUserID()).isEqualTo("demo");
		assertThat(user.getDisplayName()).isEqualTo("Demo User");
		verify(userResults).close();
		verify(context).close();
	}
}
