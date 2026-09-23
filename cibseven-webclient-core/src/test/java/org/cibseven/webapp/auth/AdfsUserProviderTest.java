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
 * The ADFS/OIDC login: exchange the authorization code with the identity provider, then mint the
 * webclient's own JWT from the claims. The identity provider is a {@link MockWebServer} issuing
 * tokens signed with a key generated here, so the whole chain verifies for real.
 */
public class AdfsUserProviderTest {

	private static final String SECRET = Base64.getEncoder().encodeToString(new byte[64]);

	private MockWebServer server;
	private RSAPrivateKey providerKey;
	private String jwks;
	private AdfsUserProvider provider;
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

		provider = new AdfsUserProvider();
		ReflectionTestUtils.setField(provider, "tokenEndpoint", server.url("/token").toString());
		ReflectionTestUtils.setField(provider, "certEndpoint", server.url("/certs").toString());
		ReflectionTestUtils.setField(provider, "domain", "example.com");
		ReflectionTestUtils.setField(provider, "clientId", "cibseven");
		ReflectionTestUtils.setField(provider, "clientSecret", "s3cret");
		ReflectionTestUtils.setField(provider, "userIdProperty", "sub");
		ReflectionTestUtils.setField(provider, "userNameProperty", "name");
		ReflectionTestUtils.setField(provider, "infoInIdToken", true);
		ReflectionTestUtils.setField(provider, "technicalUserId", "technical");
		ReflectionTestUtils.setField(provider, "secret", SECRET);
		ReflectionTestUtils.setField(provider, "validMinutes", 60L);
		ReflectionTestUtils.setField(provider, "prolongMinutes", 30L);

		enqueueJson(jwks); // consumed by the SsoHelper's KeyResolver during init
		provider.init();
		server.takeRequest(); // drain the JWKS fetch so takeRequest() below sees the token call

		request = mock(HttpServletRequest.class);
	}

	@AfterEach
	void tearDown() throws Exception {
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

	/** An identity-provider token carrying the claims AdfsUserProvider reads. */
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

	// ---------- login by authorization code ----------

	@Test
	void login_exchangesTheCodeAndBuildsAUserFromTheIdTokenClaims() throws Exception {
		String nonce = "nonce-1";
		String token = idToken(hashed(nonce));
		enqueueJson("{\"access_token\":\"opaque\",\"id_token\":\"" + token
			+ "\",\"refresh_token\":\"the-refresh\"}");
		SSOLogin login = new SSOLogin();
		login.setCode("the-code");
		login.setRedirectUrl("https://app/callback");
		login.setNonce(nonce);

		SSOUser user = (SSOUser) provider.login(login, request);

		assertThat(user.getUserID()).isEqualTo("demo");
		assertThat(user.getDisplayName()).isEqualTo("Demo User");
		assertThat(user.getAuthToken()).isNotBlank();
	}

	@Test
	void login_doesNotLeakTheRefreshTokenToTheCaller() throws Exception {
		String nonce = "nonce-1";
		String token = idToken(hashed(nonce));
		enqueueJson("{\"access_token\":\"opaque\",\"id_token\":\"" + token
			+ "\",\"refresh_token\":\"the-refresh\"}");
		SSOLogin login = new SSOLogin();
		login.setCode("the-code");
		login.setRedirectUrl("https://app/callback");
		login.setNonce(nonce);

		SSOUser user = (SSOUser) provider.login(login, request);

		// the refresh token is baked into the webclient's own token, then cleared off the object
		assertThat(user.getRefreshToken()).isNull();
	}

	@Test
	void login_takesTheEngineFromTheRequestHeader() throws Exception {
		String nonce = "nonce-1";
		enqueueJson("{\"access_token\":\"opaque\",\"id_token\":\"" + idToken(hashed(nonce)) + "\"}");
		when(request.getHeader("X-Process-Engine")).thenReturn("http://other|/engine-rest|remote");
		SSOLogin login = new SSOLogin();
		login.setCode("the-code");
		login.setRedirectUrl("https://app/callback");
		login.setNonce(nonce);

		SSOUser user = (SSOUser) provider.login(login, request);

		assertThat(user.getEngine()).isEqualTo("http://other|/engine-rest|remote");
	}

	// ---------- login by username and password ----------

	@Test
	void login_withAUsernameQualifiesItWithTheConfiguredDomain() throws Exception {
		String token = idToken("ignored");
		enqueueJson("{\"access_token\":\"" + token + "\",\"id_token\":\"" + token + "\"}");
		SSOLogin login = new SSOLogin();
		login.setUsername("demo");
		login.setPassword("demo");

		provider.login(login, request);

		// a bare username is not routable; the configured domain is appended
		assertThat(server.takeRequest().getBody().readUtf8()).contains("username=demo%40example.com");
	}

	@Test
	void login_withAQualifiedUsernameLeavesItAlone() throws Exception {
		String token = idToken("ignored");
		enqueueJson("{\"access_token\":\"" + token + "\",\"id_token\":\"" + token + "\"}");
		SSOLogin login = new SSOLogin();
		login.setUsername("demo@elsewhere.com");
		login.setPassword("demo");

		provider.login(login, request);

		assertThat(server.takeRequest().getBody().readUtf8()).contains("demo%40elsewhere.com");
	}

	@Test
	void login_leavesADomainQualifiedWindowsNameAlone() throws Exception {
		String token = idToken("ignored");
		enqueueJson("{\"access_token\":\"" + token + "\",\"id_token\":\"" + token + "\"}");
		SSOLogin login = new SSOLogin();
		login.setUsername("CORP\\demo");
		login.setPassword("demo");

		provider.login(login, request);

		assertThat(server.takeRequest().getBody().readUtf8()).contains("CORP");
	}

	// ---------- user info ----------

	@Test
	void getUserInfo_clearsTheRefreshTokenBeforeHandingTheUserOut() {
		SSOUser user = new SSOUser("demo");
		user.setRefreshToken("the-refresh");

		SSOUser result = (SSOUser) provider.getUserInfo(user, "demo");

		// the refresh token must never reach the browser
		assertThat(result.getRefreshToken()).isNull();
	}

	@Test
	void getUserInfo_refusesAnotherUsersInfo() {
		assertThatThrownBy(() -> provider.getUserInfo(new SSOUser("demo"), "someone-else"))
			.isInstanceOf(SystemException.class);
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
		user.setDisplayName("Demo User");
		user.setRefreshToken("the-refresh");

		SSOUser restored = (SSOUser) provider.deserialize(provider.serialize(user), "Bearer t");

		assertThat(restored.getUserID()).isEqualTo("demo");
		assertThat(restored.getRefreshToken()).isEqualTo("the-refresh");
		assertThat(restored.getAuthToken()).isEqualTo("Bearer t");
	}

	@Test
	void deserialize_wrapsUnreadableJsonInSystemException() {
		assertThatThrownBy(() -> provider.deserialize("not json", null))
			.isInstanceOf(SystemException.class);
	}

	// ---------- parsing the webclient's own token ----------

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
	void parse_rejectsATokenSignedWithAnotherSecret() {
		String foreign = Jwts.builder()
			.claim("user", provider.serialize(new SSOUser("demo")))
			.claim("verify", Boolean.FALSE)
			.claim("prolongable", Boolean.FALSE)
			.expiration(new Date(System.currentTimeMillis() + 600_000))
			.signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
				Base64.getDecoder().decode(Base64.getEncoder().encodeToString(new byte[64]).replace('A', 'B'))))
			.compact();

		assertThatThrownBy(() -> provider.parse(foreign, provider.getSettings()))
			.isInstanceOf(AuthenticationException.class);
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

		SSOUser refreshed = (SSOUser) provider.verify(claims);

		assertThat(refreshed.getUserID()).isEqualTo("demo");
		assertThat(refreshed.getRefreshToken()).isEqualTo("new-refresh");
	}
}
