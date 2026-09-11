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
import static org.mockito.Mockito.when;

import java.util.Base64;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.exception.SystemException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;

/**
 * The pass-through provider used when the engine itself is not asked to verify credentials: it
 * mints a token for whoever logs in and delegates session questions to an external admin/webclient
 * URL. Those two URLs are {@code @Value} fields, so a {@link MockWebServer} can stand in for them.
 */
public class GenericUserProviderTest {

	private static final String SECRET = Base64.getEncoder().encodeToString(new byte[64]);

	private MockWebServer server;
	private GenericUserProvider provider;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() throws Exception {
		server = new MockWebServer();
		QueueDispatcher dispatcher = new QueueDispatcher();
		dispatcher.setFailFast(true);
		server.setDispatcher(dispatcher);
		server.start();

		provider = new GenericUserProvider();
		ReflectionTestUtils.setField(provider, "secret", SECRET);
		ReflectionTestUtils.setField(provider, "cibsevenAdminUrl", server.url("/admin").toString());
		ReflectionTestUtils.setField(provider, "cibsevenWebclientUrl", server.url("/webclient").toString());
		ReflectionTestUtils.setField(provider, "validMinutes", 60L);
		ReflectionTestUtils.setField(provider, "prolongMinutes", 30L);
		provider.init();

		request = mock(HttpServletRequest.class);
	}

	@AfterEach
	void tearDown() throws Exception {
		server.shutdown();
	}

	private void enqueueJson(String body) {
		server.enqueue(new MockResponse().setBody(body).addHeader("Content-Type", "application/json"));
	}

	private static String bare(String authToken) {
		return authToken.startsWith("Bearer ") ? authToken.substring("Bearer ".length()) : authToken;
	}

	// ---------- login ----------

	@Test
	void login_mintsATokenForWhoeverLogsIn() {
		CIBUser user = (CIBUser) provider.login(new StandardLogin("demo", "anything"), request);

		// this provider does not verify the password itself
		assertThat(user.getUserID()).isEqualTo("demo");
		assertThat(user.getAuthToken()).isNotBlank();
	}

	@Test
	void login_takesTheEngineFromTheRequestHeader() {
		when(request.getHeader("X-Process-Engine")).thenReturn("http://other|/engine-rest|remote");

		CIBUser user = (CIBUser) provider.login(new StandardLogin("demo", "x"), request);

		assertThat(user.getEngine()).isEqualTo("http://other|/engine-rest|remote");
	}

	@Test
	void login_returnsNullWithoutCredentials() {
		assertThat(provider.login(null, request)).isNull();
	}

	@Test
	void logout_isANoOp() {
		provider.logout(new CIBUser("demo"));
	}

	@Test
	void createLoginParams_producesAStandardLogin() {
		assertThat(provider.createLoginParams()).isInstanceOf(StandardLogin.class);
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
	void verify_fromClaimsIsAlwaysNullForThisProvider() {
		assertThat(provider.verify((io.jsonwebtoken.Claims) null)).isNull();
	}

	// ---------- session lookup against the admin application ----------

	@Test
	void getSelfInfoJSessionId_mintsATokenWhenTheAdminAppKnowsTheSession() throws Exception {
		enqueueJson("{\"userId\":\"demo\",\"authorizedApps\":[\"cockpit\"]}");

		CIBUser user = (CIBUser) provider.getSelfInfoJSessionId("demo", "the-session", request);

		assertThat(user.getUserID()).isEqualTo("demo");
		assertThat(user.getAuthToken()).isNotBlank();
		var recorded = server.takeRequest();
		assertThat(recorded.getPath()).endsWith("/admin/auth/user/default");
		assertThat(recorded.getHeader("Cookie")).isEqualTo("JSESSIONID=the-session");
	}

	@Test
	void getSelfInfoJSessionId_returnsNullWhenTheSessionBelongsToSomeoneElse() {
		enqueueJson("{\"userId\":\"someone-else\"}");

		// the session is valid but not this user's, so no token is issued
		assertThat(provider.getSelfInfoJSessionId("demo", "the-session", request)).isNull();
	}

	@Test
	void getSelfInfoJSessionId_returnsNullWhenTheAdminAppSendsNoBody() {
		server.enqueue(new MockResponse().setResponseCode(204));

		assertThat(provider.getSelfInfoJSessionId("demo", "the-session", request)).isNull();
	}

	@Test
	void getSelfInfoJSessionId_turnsAnAdminAppErrorIntoAnAuthenticationException() {
		server.enqueue(new MockResponse().setResponseCode(401).setBody("{}"));

		assertThatThrownBy(() -> provider.getSelfInfoJSessionId("demo", "the-session", request))
			.isInstanceOf(AuthenticationException.class);
	}

	// ---------- token verification against the webclient ----------

	@Test
	void verifyToken_returnsTheTokenTheWebclientHandsBack() throws Exception {
		enqueueJson("{\"userID\":\"demo\",\"authToken\":\"Bearer renewed\"}");

		assertThat(provider.verify("the-token")).isEqualTo("Bearer renewed");
		var recorded = server.takeRequest();
		assertThat(recorded.getPath()).endsWith("/webclient/auth");
		assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer the-token");
	}

	@Test
	void verifyToken_isNullWhenNoWebclientUrlIsConfigured() {
		ReflectionTestUtils.setField(provider, "cibsevenWebclientUrl", "");

		assertThat(provider.verify("the-token")).isNull();
	}

	@Test
	void verifyToken_extractsTheRenewedTokenFromATokenExpiredResponse() {
		// the webclient answers 4xx with a structured error carrying the replacement token
		server.enqueue(new MockResponse().setResponseCode(401)
			.setBody("{\"type\":\"TokenExpiredException\",\"params\":[\"the-renewed-token\"]}")
			.addHeader("Content-Type", "application/json"));

		assertThat(provider.verify("expired-token")).isEqualTo("the-renewed-token");
	}

	@Test
	void verifyToken_isNullForAnyOtherErrorType() {
		server.enqueue(new MockResponse().setResponseCode(401)
			.setBody("{\"type\":\"AuthenticationException\",\"params\":[]}")
			.addHeader("Content-Type", "application/json"));

		assertThat(provider.verify("bad-token")).isNull();
	}

	@Test
	void verifyToken_isNullWhenTheErrorBodyCannotBeParsed() {
		server.enqueue(new MockResponse().setResponseCode(401).setBody("not json"));

		assertThat(provider.verify("bad-token")).isNull();
	}

	// ---------- serialization and parsing ----------

	@Test
	void serializeThenDeserialize_roundTripsTheUser() {
		CIBUser user = new CIBUser("demo");
		user.setDisplayName("Demo User");

		CIBUser restored = (CIBUser) provider.deserialize(provider.serialize(user), "Bearer t");

		assertThat(restored.getUserID()).isEqualTo("demo");
		assertThat(restored.getAuthToken()).isEqualTo("Bearer t");
	}

	@Test
	void deserialize_ignoresUnknownProperties() {
		CIBUser restored =
			(CIBUser) provider.deserialize("{\"userID\":\"demo\",\"somethingNew\":true}", null);

		// tolerating unknown fields lets an older node read a newer node's token
		assertThat(restored.getUserID()).isEqualTo("demo");
	}

	@Test
	void deserialize_wrapsUnreadableJsonInSystemException() {
		assertThatThrownBy(() -> provider.deserialize("not json", null))
			.isInstanceOf(SystemException.class);
	}

	@Test
	void parse_readsBackAUserFromATokenItIssued() {
		CIBUser user = (CIBUser) provider.login(new StandardLogin("demo", "x"), request);

		CIBUser parsed = (CIBUser) provider.parse(bare(user.getAuthToken()), provider.getSettings());

		assertThat(parsed.getUserID()).isEqualTo("demo");
	}

	@Test
	void parse_rejectsSomethingThatIsNotAToken() {
		assertThatThrownBy(() -> provider.parse("not-a-token", provider.getSettings()))
			.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void parse_reportsAnExpiredTokenAsExpired() {
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
	void parse_renewsAProlongableExpiredTokenThroughTheWebclient() {
		enqueueJson("{\"userID\":\"demo\",\"authToken\":\"Bearer renewed\"}");
		String token = io.jsonwebtoken.Jwts.builder()
			.claim("user", provider.serialize(new CIBUser("demo")))
			.claim("verify", Boolean.FALSE)
			.claim("prolongable", Boolean.TRUE)
			.expiration(new java.util.Date(System.currentTimeMillis() - 300_000))
			.signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
			.compact();

		TokenExpiredException thrown = (TokenExpiredException) org.assertj.core.api.Assertions
			.catchThrowable(() -> provider.parse(token, provider.getSettings()));

		assertThat(thrown.getData()).containsExactly("Bearer renewed");
	}
}
