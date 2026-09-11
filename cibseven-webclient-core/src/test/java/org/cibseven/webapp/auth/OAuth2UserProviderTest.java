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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.auth.sso.SSOLogin;
import org.cibseven.webapp.auth.sso.SSOUser;
import org.cibseven.webapp.exception.SystemException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;

/**
 * The generic OIDC provider. Beyond the ADFS flow it also accepts a third-party access token
 * directly - resolving the user from the userinfo or introspection endpoint - and can forward that
 * access token to the engine, caching it per refresh token.
 */
public class OAuth2UserProviderTest {

	private static final String SECRET = Base64.getEncoder().encodeToString(new byte[64]);

	private MockWebServer server;
	private RSAPrivateKey providerKey;
	private String jwks;
	private OAuth2UserProvider provider;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		providerKey = (RSAPrivateKey) keyPair.getPrivate();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		jwks = "{\"keys\":[{\"kid\":\"k\",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\","
			+ "\"n\":\"" + base64Url(publicKey.getModulus()) + "\","
			+ "\"e\":\"" + base64Url(publicKey.getPublicExponent()) + "\"}]}";

		server = new MockWebServer();
		QueueDispatcher dispatcher = new QueueDispatcher();
		dispatcher.setFailFast(true);
		server.setDispatcher(dispatcher);
		server.start();

		provider = newProvider(false);
		request = mock(HttpServletRequest.class);
	}

	private OAuth2UserProvider newProvider(boolean forwardToken) throws Exception {
		OAuth2UserProvider created = new OAuth2UserProvider();
		ReflectionTestUtils.setField(created, "tokenEndpoint", server.url("/token").toString());
		ReflectionTestUtils.setField(created, "certEndpoint", server.url("/certs").toString());
		ReflectionTestUtils.setField(created, "userEndpoint", server.url("/userinfo").toString());
		ReflectionTestUtils.setField(created, "introspectionEndpoint", server.url("/introspect").toString());
		ReflectionTestUtils.setField(created, "clientId", "cibseven");
		ReflectionTestUtils.setField(created, "clientSecret", "s3cret");
		ReflectionTestUtils.setField(created, "userIdProperty", "sub");
		ReflectionTestUtils.setField(created, "userNameProperty", "name");
		ReflectionTestUtils.setField(created, "forwardToken", forwardToken);
		ReflectionTestUtils.setField(created, "secret", SECRET);
		ReflectionTestUtils.setField(created, "validMinutes", 60L);
		ReflectionTestUtils.setField(created, "prolongMinutes", 30L);
		enqueueJson(jwks);
		created.init();
		server.takeRequest(); // drain the JWKS fetch
		return created;
	}

	@AfterEach
	void tearDown() throws Exception {
		provider.destroy();
		server.shutdown();
	}

	private static String base64Url(BigInteger value) {
		byte[] bytes = value.toByteArray();
		if (bytes.length > 1 && bytes[0] == 0) {
			byte[] trimmed = new byte[bytes.length - 1];
			System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
			bytes = trimmed;
		}
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private void enqueueJson(String body) {
		server.enqueue(new MockResponse().setBody(body).addHeader("Content-Type", "application/json"));
	}

	private static String hashed(String nonce) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(nonce.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();
		for (byte b : digest) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private String idToken(String nonce) {
		return Jwts.builder()
			.header().keyId("k").and()
			.subject("demo")
			.claim("nonce", nonce)
			.claim("name", "Demo User")
			.expiration(new Date(System.currentTimeMillis() + 600_000))
			.signWith(providerKey, Jwts.SIG.RS256)
			.compact();
	}

	private static String bare(String authToken) {
		return authToken.startsWith("Bearer ") ? authToken.substring("Bearer ".length()) : authToken;
	}

	// ---------- configuration ----------

	@Test
	void init_requiresEitherASecretOrAnAssertionType() {
		OAuth2UserProvider misconfigured = new OAuth2UserProvider();
		ReflectionTestUtils.setField(misconfigured, "clientSecret", "");
		ReflectionTestUtils.setField(misconfigured, "assertionType", null);

		assertThatThrownBy(misconfigured::init)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Either clientSecret must be provided");
	}

	@Test
	void destroy_isSafeWhenNoSchedulerWasStarted() {
		// forwardToken is off in this fixture, so there is no scheduler to shut down
		provider.destroy();
	}

	// ---------- login by authorization code ----------

	@Test
	void login_exchangesTheCodeAndBuildsTheUserFromTheIdToken() throws Exception {
		String nonce = "nonce-1";
		enqueueJson("{\"access_token\":\"opaque\",\"id_token\":\"" + idToken(hashed(nonce))
			+ "\",\"refresh_token\":\"the-refresh\"}");
		SSOLogin login = new SSOLogin();
		login.setCode("the-code");
		login.setRedirectUrl("https://app/callback");
		login.setNonce(nonce);

		SSOUser user = (SSOUser) provider.login(login, request);

		assertThat(user.getUserID()).isEqualTo("demo");
		assertThat(user.getDisplayName()).isEqualTo("Demo User");
		assertThat(user.getRefreshToken()).isNull();
	}

	// ---------- login with a third-party access token ----------

	@Test
	void login_withAnAccessTokenResolvesTheUserFromUserinfo() {
		enqueueJson("{\"sub\":\"demo\",\"name\":\"Demo User\"}");
		// getTokenExpiration falls back to introspection for an opaque token
		enqueueJson("{\"active\":true,\"exp\":" + (System.currentTimeMillis() / 1000 + 600) + "}");
		SSOLogin login = new SSOLogin();
		login.setAuthToken("third-party-token");

		SSOUser user = (SSOUser) provider.login(login, request);

		assertThat(user.getUserID()).isEqualTo("demo");
		assertThat(user.getDisplayName()).isEqualTo("Demo User");
		assertThat(user.getAuthToken()).isNotBlank();
	}

	@Test
	void login_withAnAccessTokenFallsBackToIntrospectionWhenUserinfoIsForbidden() {
		server.enqueue(new MockResponse().setResponseCode(403).setBody("{}"));
		enqueueJson("{\"active\":true,\"sub\":\"technical-user\"}");
		// then getTokenExpiration introspects again
		enqueueJson("{\"active\":true,\"exp\":" + (System.currentTimeMillis() / 1000 + 600) + "}");
		SSOLogin login = new SSOLogin();
		login.setAuthToken("technical-token");

		SSOUser user = (SSOUser) provider.login(login, request);

		// a technical user has no profile, so the subject becomes both id and display name
		assertThat(user.getUserID()).isEqualTo("technical-user");
		assertThat(user.getDisplayName()).isEqualTo("technical-user");
	}

	@Test
	void login_withAnAccessTokenRejectsAnUnidentifiableCaller() {
		server.enqueue(new MockResponse().setResponseCode(403).setBody("{}"));
		enqueueJson("{\"active\":false}");
		SSOLogin login = new SSOLogin();
		login.setAuthToken("anonymous-token");

		assertThatThrownBy(() -> provider.login(login, request))
			.isInstanceOf(AuthenticationException.class);
	}

	// ---------- user info ----------

	@Test
	void getUserInfo_clearsTheRefreshTokenBeforeHandingTheUserOut() {
		SSOUser user = new SSOUser("demo");
		user.setRefreshToken("the-refresh");

		assertThat(((SSOUser) provider.getUserInfo(user, "demo")).getRefreshToken()).isNull();
	}

	@Test
	void getUserInfo_refusesAnotherUsersInfo() {
		assertThatThrownBy(() -> provider.getUserInfo(new SSOUser("demo"), "someone-else"))
			.isInstanceOf(SystemException.class);
	}

	@Test
	void getSelfInfoJSessionId_isNotSupported() {
		assertThat(provider.getSelfInfoJSessionId("demo", "session", request)).isNull();
	}

	@Test
	void logout_isANoOp() {
		provider.logout(new SSOUser("demo"));
	}

	@Test
	void createLoginParams_producesAnSsoLogin() {
		assertThat(provider.createLoginParams()).isInstanceOf(SSOLogin.class);
	}

	// ---------- serialization ----------

	@Test
	void serializeThenDeserialize_roundTripsTheSsoUser() {
		SSOUser user = new SSOUser("demo");
		user.setRefreshToken("the-refresh");

		SSOUser restored = (SSOUser) provider.deserialize(provider.serialize(user), "Bearer t");

		assertThat(restored.getUserID()).isEqualTo("demo");
		assertThat(restored.getRefreshToken()).isEqualTo("the-refresh");
	}

	@Test
	void deserialize_wrapsUnreadableJsonInSystemException() {
		assertThatThrownBy(() -> provider.deserialize("not json", null))
			.isInstanceOf(SystemException.class);
	}

	// ---------- parsing ----------

	@Test
	void parse_readsBackAUserFromATokenItIssued() throws Exception {
		String nonce = "nonce-1";
		enqueueJson("{\"access_token\":\"opaque\",\"id_token\":\"" + idToken(hashed(nonce)) + "\"}");
		SSOLogin login = new SSOLogin();
		login.setCode("the-code");
		login.setRedirectUrl("https://app/callback");
		login.setNonce(nonce);
		SSOUser user = (SSOUser) provider.login(login, request);

		SSOUser parsed = (SSOUser) provider.parse(bare(user.getAuthToken()), provider.getSettings());

		assertThat(parsed.getUserID()).isEqualTo("demo");
	}

	@Test
	void parse_treatsAForeignTokenAsAThirdPartyAccessToken() {
		enqueueJson("{\"sub\":\"demo\",\"name\":\"Demo User\"}");

		// not one of ours, so the userinfo endpoint decides who this is
		SSOUser user = (SSOUser) provider.parse("a-third-party-opaque-token", provider.getSettings());

		assertThat(user.getUserID()).isEqualTo("demo");
	}

	@Test
	void parse_reportsAnExpiredNonProlongableTokenAsExpired() {
		String token = Jwts.builder()
			.claim("user", provider.serialize(new SSOUser("demo")))
			.claim("verify", Boolean.FALSE)
			.claim("prolongable", Boolean.FALSE)
			.expiration(new Date(System.currentTimeMillis() - 300_000))
			.signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
			.compact();

		assertThatThrownBy(() -> provider.parse(token, provider.getSettings()))
			.isInstanceOf(TokenExpiredException.class);
	}

	@Test
	void verify_refreshesTheSessionAgainstTheIdentityProvider() {
		SSOUser user = new SSOUser("demo");
		user.setRefreshToken("the-refresh");
		io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
		when(claims.get("user", String.class)).thenReturn(provider.serialize(user));
		enqueueJson("{\"access_token\":\"new-access\",\"refresh_token\":\"new-refresh\"}");

		assertThat(((SSOUser) provider.verify(claims)).getRefreshToken()).isEqualTo("new-refresh");
	}

	// ---------- forwarding the access token to the engine ----------

	@Test
	void getEngineRestToken_returnsTheWebclientTokenWhenForwardingIsOff() {
		SSOUser user = new SSOUser("demo");
		user.setAuthToken("Bearer our-own-token");

		// with forwarding off the engine gets the webclient's token, not the provider's
		assertThat(provider.getEngineRestToken(user)).isEqualTo("Bearer our-own-token");
	}

	@Test
	void getEngineRestToken_fetchesAndForwardsTheProvidersAccessToken() throws Exception {
		provider.destroy();
		provider = newProvider(true);
		SSOUser user = new SSOUser("demo");
		user.setRefreshToken("the-refresh");
		enqueueJson("{\"access_token\":\"provider-access\",\"refresh_token\":\"the-refresh\"}");
		enqueueJson("{\"active\":true,\"exp\":" + (System.currentTimeMillis() / 1000 + 600) + "}");

		assertThat(provider.getEngineRestToken(user)).isEqualTo("Bearer provider-access");
	}

	@Test
	void getEngineRestToken_servesTheCachedAccessTokenOnTheSecondCall() throws Exception {
		provider.destroy();
		provider = newProvider(true);
		SSOUser user = new SSOUser("demo");
		user.setRefreshToken("the-refresh");
		enqueueJson("{\"access_token\":\"provider-access\",\"refresh_token\":\"the-refresh\"}");
		enqueueJson("{\"active\":true,\"exp\":" + (System.currentTimeMillis() / 1000 + 600) + "}");

		provider.getEngineRestToken(user);
		int afterFirst = server.getRequestCount();
		String second = provider.getEngineRestToken(user);

		// the cached token is still valid, so no further round-trip to the provider
		assertThat(second).isEqualTo("Bearer provider-access");
		assertThat(server.getRequestCount()).isEqualTo(afterFirst);
	}
}
