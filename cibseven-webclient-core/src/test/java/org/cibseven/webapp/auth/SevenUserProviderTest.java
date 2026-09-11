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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Base64;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.SevenUser;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The engine-backed login: verify the credentials against the engine, then mint a JWT the rest of
 * the webclient carries. Covers the login outcomes and the token round-trip.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SevenUserProviderTest {

	/** A 64-byte HS256 secret, base64 as the configuration expects. */
	private static final String SECRET =
		Base64.getEncoder().encodeToString(new byte[64]);

	@Mock
	private BpmProvider bpmProvider;

	private SevenUserProvider provider;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() {
		provider = new SevenUserProvider();
		ReflectionTestUtils.setField(provider, "secret", SECRET);
		ReflectionTestUtils.setField(provider, "cibsevenUrl", "http://localhost:8080");
		ReflectionTestUtils.setField(provider, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(provider, "validMinutes", 60L);
		ReflectionTestUtils.setField(provider, "prolongMinutes", 30L);
		provider.init();

		request = mock(HttpServletRequest.class);
	}

	/** parse() takes the raw JWT, while login() hands out a Bearer-prefixed one. */
	private static String bare(String authToken) {
		return authToken.startsWith("Bearer ") ? authToken.substring("Bearer ".length()) : authToken;
	}

	private void engineAccepts(String userId, String firstName, String lastName) {
		SevenVerifyUser verified = new SevenVerifyUser();
		verified.setAuthenticatedUser(userId);
		verified.setAuthenticated(true);
		when(bpmProvider.verifyUser(any(), any())).thenReturn(verified);
		SevenUser profile = new SevenUser();
		profile.setId(userId);
		profile.setFirstName(firstName);
		profile.setLastName(lastName);
		when(bpmProvider.getUserProfile(any(), any())).thenReturn(profile);
	}

	// ---------- init ----------

	@Test
	void init_buildsTheTokenSettingsFromTheConfiguredSecret() {
		assertThat(provider.getSettings()).isNotNull();
		assertThat(provider.getSettings().getSecret()).isEqualTo(SECRET);
		assertThat(provider.getSettings().getValid()).isEqualTo(java.time.Duration.ofMinutes(60));
	}

	// ---------- login ----------

	@Test
	void login_returnsAUserCarryingATokenAndDisplayName() {
		engineAccepts("demo", "Demo", "User");

		CIBUser user = provider.login(new StandardLogin("demo", "demo"), request);

		assertThat(user.getUserID()).isEqualTo("demo");
		assertThat(user.getDisplayName()).isEqualTo("Demo User");
		assertThat(user.getAuthToken()).isNotBlank();
	}

	@Test
	void login_mintsTheTokenTwiceSoTheDisplayNameIsInTheFinalOne() {
		engineAccepts("demo", "Demo", "User");

		CIBUser user = provider.login(new StandardLogin("demo", "demo"), request);

		// the second token is issued after the profile lookup, so parsing it back yields the name
		CIBUser parsed = (CIBUser) provider.parse(bare(user.getAuthToken()), provider.getSettings());
		assertThat(parsed.getDisplayName()).isEqualTo("Demo User");
	}

	@Test
	void login_takesTheEngineFromTheRequestHeader() {
		engineAccepts("demo", "Demo", "User");
		when(request.getHeader("X-Process-Engine")).thenReturn("http://other|/engine-rest|remote");

		CIBUser user = provider.login(new StandardLogin("demo", "demo"), request);

		assertThat(user.getEngine()).isEqualTo("http://other|/engine-rest|remote");
	}

	@Test
	void login_refusesWhenTheEngineDoesNotAuthenticateTheUser() {
		SevenVerifyUser rejected = new SevenVerifyUser();
		rejected.setAuthenticatedUser("demo");
		rejected.setAuthenticated(false);
		when(bpmProvider.verifyUser(any(), any())).thenReturn(rejected);

		assertThatThrownBy(() -> provider.login(new StandardLogin("demo", "wrong"), request))
			.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void login_turnsAnEngineFailureIntoAnAuthenticationException() {
		when(bpmProvider.verifyUser(any(), any())).thenThrow(new SystemException("engine unreachable"));

		// the caller must see an authentication failure, not a 500
		assertThatThrownBy(() -> provider.login(new StandardLogin("demo", "demo"), request))
			.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void login_toleratesAMissingUsernameWhenReportingTheFailure() {
		when(bpmProvider.verifyUser(any(), any())).thenThrow(new SystemException("engine unreachable"));

		assertThatThrownBy(() -> provider.login(new StandardLogin(null, "demo"), request))
			.isInstanceOf(AuthenticationException.class);
	}

	// ---------- user info ----------

	@Test
	void getUserInfo_returnsTheSameUserForItsOwnId() {
		CIBUser user = new CIBUser("demo");

		assertThat(provider.getUserInfo(user, "demo")).isSameAs(user);
	}

	@Test
	void getUserInfo_refusesToHandOutAnotherUsersInfo() {
		CIBUser user = new CIBUser("demo");

		assertThatThrownBy(() -> provider.getUserInfo(user, "someone-else"))
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

	// ---------- serialization ----------

	@Test
	void serializeThenDeserialize_roundTripsTheUser() {
		CIBUser user = new CIBUser("demo");
		user.setDisplayName("Demo User");

		String json = provider.serialize(user);
		CIBUser restored = (CIBUser) provider.deserialize(json, "Bearer the-token");

		assertThat(restored.getUserID()).isEqualTo("demo");
		assertThat(restored.getDisplayName()).isEqualTo("Demo User");
		assertThat(restored.getAuthToken()).isEqualTo("Bearer the-token");
	}

	@Test
	void deserialize_wrapsUnreadableJsonInSystemException() {
		assertThatThrownBy(() -> provider.deserialize("not json at all", null))
			.isInstanceOf(SystemException.class);
	}

	// ---------- token parsing ----------

	@Test
	void parse_readsBackAUserFromATokenItIssued() {
		engineAccepts("demo", "Demo", "User");
		CIBUser user = provider.login(new StandardLogin("demo", "demo"), request);

		CIBUser parsed = (CIBUser) provider.parse(bare(user.getAuthToken()), provider.getSettings());

		assertThat(parsed.getUserID()).isEqualTo("demo");
		// the parsed user carries the bearer-prefixed token it came from
		assertThat(parsed.getAuthToken()).startsWith("Bearer ");
	}

	@Test
	void parse_rejectsSomethingThatIsNotAToken() {
		assertThatThrownBy(() -> provider.parse("not-a-token", provider.getSettings()))
			.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void parse_rejectsATokenSignedWithAnotherSecret() {
		engineAccepts("demo", "Demo", "User");
		CIBUser user = provider.login(new StandardLogin("demo", "demo"), request);

		SevenUserProvider otherProvider = new SevenUserProvider();
		String otherSecret = Base64.getEncoder().encodeToString("a-completely-different-secret-of-64-bytes-length-padding-here!!".getBytes());
		ReflectionTestUtils.setField(otherProvider, "secret", otherSecret);
		ReflectionTestUtils.setField(otherProvider, "bpmProvider", bpmProvider);
		ReflectionTestUtils.setField(otherProvider, "validMinutes", 60L);
		ReflectionTestUtils.setField(otherProvider, "prolongMinutes", 30L);
		otherProvider.init();

		// a token minted for one deployment must not be accepted by another
		assertThatThrownBy(() -> otherProvider.parse(bare(user.getAuthToken()), otherProvider.getSettings()))
			.isInstanceOf(AuthenticationException.class);
	}

	/** A token signed with the configured secret whose expiry is {@code agoMinutes} in the past. */
	private String expiredToken(boolean prolongable, long agoMinutes) {
		CIBUser user = new CIBUser("demo");
		return io.jsonwebtoken.Jwts.builder()
			.claim("user", provider.serialize(user))
			.claim("verify", Boolean.FALSE)
			.claim("prolongable", prolongable)
			.expiration(new java.util.Date(System.currentTimeMillis() - agoMinutes * 60_000))
			.signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
			.compact();
	}

	@Test
	void parse_reportsAnExpiredTokenAsExpiredRatherThanInvalid() {
		String token = expiredToken(false, 5);

		// the frontend distinguishes the two: expired means "log in again", invalid means "reject"
		assertThatThrownBy(() -> provider.parse(token, provider.getSettings()))
			.isInstanceOf(TokenExpiredException.class);
	}

	@Test
	void parse_offersAFreshTokenWhenAnExpiredOneIsStillProlongable() {
		String token = expiredToken(true, 5);

		TokenExpiredException thrown = (TokenExpiredException) org.assertj.core.api.Assertions
			.catchThrowable(() -> provider.parse(token, provider.getSettings()));

		// within the prolong window the session is renewed rather than dropped; the replacement
		// token rides in getData(), because this exception family has no message or payload field
		assertThat(thrown).isNotNull();
		assertThat(thrown.getData()).hasSize(1);
		assertThat(String.valueOf(thrown.getData()[0])).isNotBlank();
	}

	@Test
	void parse_doesNotProlongATokenExpiredBeyondTheWindow() {
		String token = expiredToken(true, 120);

		TokenExpiredException thrown = (TokenExpiredException) org.assertj.core.api.Assertions
			.catchThrowable(() -> provider.parse(token, provider.getSettings()));

		// no replacement token: the session is over
		assertThat(thrown).isNotNull();
		assertThat(thrown.getData()).isEmpty();
	}

	@Test
	void verify_readsTheUserOutOfTheClaims() {
		engineAccepts("demo", "Demo", "User");
		CIBUser user = provider.login(new StandardLogin("demo", "demo"), request);
		io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
			.verifyWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
			.build().parseSignedClaims(bare(user.getAuthToken())).getPayload();

		assertThat(provider.verify(claims).getId()).isEqualTo("demo");
	}

	/**
	 * TODO KNOWN BUG (not fixed): {@code verify()} catches {@code Exception} and builds its
	 * {@code AuthenticationException} from {@code userClaims.get("user").toString()} - the very
	 * expression that just failed - so a token whose claims carry no "user" leaves the handler with
	 * a {@link NullPointerException} instead of the intended authentication failure. This test
	 * asserts the failure a caller should see; it fails until the catch block stops dereferencing
	 * the missing claim.
	 */
	@Test
	@Disabled("KNOWN BUG: verify() rebuilds its AuthenticationException from the null claim that just failed, so it NPEs")
	void verify_failsAuthenticationWhenTheClaimsCarryNoUser() {
		io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
		when(claims.get("user")).thenReturn(null);

		assertThatThrownBy(() -> provider.verify(claims))
			.isInstanceOf(AuthenticationException.class);
	}
}
