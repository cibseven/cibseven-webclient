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
package org.cibseven.webapp.auth.sso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import org.cibseven.webapp.auth.assertion.AssertionType;
import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Drives {@link SsoHelper} - and through its constructor {@link KeyResolver} - against a stand-in
 * identity provider.
 * <p>
 * The helper builds its own {@code RestTemplate} per call, so it cannot be given a mock transport;
 * its endpoints are constructor arguments though, so pointing them at a {@link MockWebServer} is
 * what makes it testable. The signing key is generated here and published as a JWKS, so the tokens
 * the fake provider issues verify for real rather than through a stubbed verifier.
 */
public class SsoHelperTest {

	private static final String CLIENT_ID = "cibseven";
	private static final String CLIENT_SECRET = "s3cret";

	private MockWebServer server;
	private RSAPrivateKey privateKey;
	private String jwks;

	@BeforeEach
	void setUp() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		privateKey = (RSAPrivateKey) keyPair.getPrivate();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

		jwks = "{\"keys\":[{\"kid\":\"test-key\",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\","
			+ "\"n\":\"" + base64Url(publicKey.getModulus()) + "\","
			+ "\"e\":\"" + base64Url(publicKey.getPublicExponent()) + "\"}]}";

		server = new MockWebServer();
		QueueDispatcher dispatcher = new QueueDispatcher();
		dispatcher.setFailFast(true);
		server.setDispatcher(dispatcher);
		server.start();
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

	private String url(String path) {
		return server.url(path).toString();
	}

	private void enqueueJson(String body) {
		server.enqueue(new MockResponse().setBody(body).addHeader("Content-Type", "application/json"));
	}

	/** The same SHA-256 hex hashing SsoHelper applies to the nonce before comparing it. */
	private static String hashed(String nonce) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(nonce.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();
		for (byte b : digest) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private String signedToken(String nonce) {
		return Jwts.builder()
			.header().keyId("test-key").and()
			.subject("demo")
			.claim("nonce", nonce)
			.expiration(new Date(System.currentTimeMillis() + 600_000))
			.signWith(privateKey, Jwts.SIG.RS256)
			.compact();
	}

	/** Builds the helper; consumes one enqueued response for the JWKS fetch. */
	private SsoHelper helper() throws Exception {
		enqueueJson(jwks);
		return new SsoHelper(url("/token"), CLIENT_ID, CLIENT_SECRET, null, null,
			url("/certs"), url("/userinfo"), url("/introspect"));
	}

	// ---------- construction ----------

	@Test
	void construction_fetchesTheSigningKeyUpFront() throws Exception {
		SsoHelper helper = helper();

		assertThat(helper.getKeyResolver()).isNotNull();
		assertThat(helper.getKeyResolver().getKey()).isNotNull();
		assertThat(server.takeRequest().getPath()).isEqualTo("/certs");
	}

	/**
	 * TODO KNOWN BUG (not fixed): {@code KeyResolver.loadKey} guards against a null {@code KeyList}
	 * but not against a {@code KeyList} whose "keys" array is absent, so a JWKS document that
	 * parses but has no keys - a misconfigured or half-migrated identity provider - dereferences
	 * null and fails with a bare {@link NullPointerException}. This test asserts the
	 * {@code AuthenticationException} the guard is meant to produce; it fails until the empty-key
	 * case is guarded too.
	 */
	@Test
	@Disabled("KNOWN BUG: KeyResolver.loadKey does not guard an empty JWKS, so it NPEs instead of failing authentication")
	void construction_failsWhenTheProviderPublishesNoKeys() {
		enqueueJson("{}");

		assertThatThrownBy(() -> new SsoHelper(url("/token"), CLIENT_ID, CLIENT_SECRET, null, null,
				url("/certs"), url("/userinfo"), url("/introspect")))
			.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void construction_withoutASecretRequiresAnAssertionType() {
		// no client secret means client-assertion authentication, which needs to be configured
		assertThatThrownBy(() -> new SsoHelper(url("/token"), CLIENT_ID, "  ", null, null,
				url("/certs"), url("/userinfo"), url("/introspect")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("assertion.type must be set");
	}

	@Test
	void construction_withJjwtAssertionRequiresAKeyLocation() {
		assertThatThrownBy(() -> new SsoHelper(url("/token"), CLIENT_ID, null, AssertionType.JJWT, "  ",
				url("/certs"), url("/userinfo"), url("/introspect")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("keyLocation must be set");
	}

	@Test
	void construction_reportsAMissingPemResource() {
		assertThatThrownBy(() -> new SsoHelper(url("/token"), CLIENT_ID, null, AssertionType.JJWT,
				"classpath:no-such-key.pem", url("/certs"), url("/userinfo"), url("/introspect")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("PEM resource not found");
	}

	// ---------- code exchange ----------

	@Test
	void codeExchange_sendsTheAuthorizationCodeGrantWithTheClientSecret() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest(); // the JWKS fetch
		String nonce = "nonce-1";
		String token = signedToken(hashed(nonce));
		enqueueJson("{\"access_token\":\"" + token + "\",\"id_token\":\"" + token + "\"}");

		TokenResponse tokens = helper.codeExchange("the-code", "https://app/callback", nonce);

		assertThat(tokens.getAccess_token()).isEqualTo(token);
		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath()).isEqualTo("/token");
		String body = request.getBody().readUtf8();
		assertThat(body).contains("grant_type=authorization_code")
			.contains("client_id=" + CLIENT_ID)
			.contains("client_secret=" + CLIENT_SECRET)
			.contains("code=the-code");
		// the redirect_uri has to be echoed back for the provider to accept the exchange
		assertThat(body).contains("redirect_uri=");
	}

	@Test
	void codeExchange_verifiesTheNonceInTheIdToken() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		String token = signedToken(hashed("a-different-nonce"));
		enqueueJson("{\"access_token\":\"" + token + "\",\"id_token\":\"" + token + "\"}");

		// a mismatching nonce means the response does not belong to this login attempt.
		// The reason it gives never reaches getMessage() - see
		// authenticationExceptionsShouldCarryTheProviderMessage below, which is disabled.
		assertThatThrownBy(() -> helper.codeExchange("the-code", "https://app/callback", "nonce-1"))
			.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void codeExchange_toleratesAnOpaqueAccessToken() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		String nonce = "nonce-1";
		String idToken = signedToken(hashed(nonce));
		// the access token need not be a JWT at all
		enqueueJson("{\"access_token\":\"opaque-random-string\",\"id_token\":\"" + idToken + "\"}");

		TokenResponse tokens = helper.codeExchange("the-code", "https://app/callback", nonce);

		assertThat(tokens.getAccess_token()).isEqualTo("opaque-random-string");
		assertThat(tokens.getIdClaims().getSubject()).isEqualTo("demo");
		assertThat(tokens.getAccessClaims()).isNull();
	}

	@Test
	void codeExchange_canSkipTheNonceCheckOnTheIdToken() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		String token = signedToken(hashed("whatever"));
		enqueueJson("{\"access_token\":\"opaque\",\"id_token\":\"" + token + "\"}");

		TokenResponse tokens =
			helper.codeExchange("the-code", "https://app/callback", "nonce-1", false, false);

		assertThat(tokens).isNotNull();
	}

	@Test
	void codeExchange_turnsAProviderErrorIntoAnAuthenticationException() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		server.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":\"invalid_grant\"}"));

		// the provider's response body is passed to the exception but does not survive it -
		// see authenticationExceptionsShouldCarryTheProviderMessage, which is disabled
		assertThatThrownBy(() -> helper.codeExchange("stale-code", "https://app/callback", "nonce-1"))
			.isInstanceOf(AuthenticationException.class);
	}

	// ---------- refresh ----------

	@Test
	void refreshToken_sendsTheRefreshGrant() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		enqueueJson("{\"access_token\":\"new-access\",\"refresh_token\":\"new-refresh\"}");

		TokenResponse tokens = helper.refreshToken("old-refresh");

		assertThat(tokens.getAccess_token()).isEqualTo("new-access");
		String body = server.takeRequest().getBody().readUtf8();
		assertThat(body).contains("grant_type=refresh_token").contains("refresh_token=old-refresh");
	}

	@Test
	void refreshToken_turnsAProviderErrorIntoAnAuthenticationException() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		server.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":\"invalid_grant\"}"));

		assertThatThrownBy(() -> helper.refreshToken("expired"))
			.isInstanceOf(AuthenticationException.class);
	}

	// ---------- password login ----------

	@Test
	void passwordLogin_sendsThePasswordGrantAndVerifiesBothTokens() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		String token = signedToken("ignored");
		enqueueJson("{\"access_token\":\"" + token + "\",\"id_token\":\"" + token + "\"}");

		TokenResponse tokens = helper.passwordLogin("demo", "demo");

		assertThat(tokens.getAccessClaims().getSubject()).isEqualTo("demo");
		assertThat(tokens.getIdClaims().getSubject()).isEqualTo("demo");
		String body = server.takeRequest().getBody().readUtf8();
		assertThat(body).contains("grant_type=password").contains("username=demo");
	}

	@Test
	void passwordLogin_turnsAProviderErrorIntoAnAuthenticationException() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"invalid_client\"}"));

		assertThatThrownBy(() -> helper.passwordLogin("demo", "wrong"))
			.isInstanceOf(AuthenticationException.class);
	}

	// ---------- user info ----------

	@Test
	void getUserInfo_sendsTheAccessTokenAsABearerHeader() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		enqueueJson("{\"sub\":\"demo\",\"preferred_username\":\"demo\"}");

		Map<String, String> userInfo = helper.getUserInfo("the-access-token");

		assertThat(userInfo).containsEntry("sub", "demo");
		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath()).isEqualTo("/userinfo");
		assertThat(request.getHeader("Authorization")).isEqualTo("Bearer the-access-token");
	}

	@Test
	void getUserInfo_returnsNullForATechnicalUserTheProviderRefuses() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		server.enqueue(new MockResponse().setResponseCode(403).setBody("{}"));

		// 403 means a technical user with no profile; the caller falls back to the sub claim
		assertThat(helper.getUserInfo("the-access-token")).isNull();
	}

	@Test
	void getUserInfo_rejectsAnInvalidTokenWithAnAuthenticationException() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		server.enqueue(new MockResponse().setResponseCode(401).setBody("{}"));

		// the message is lost on the way out - see authenticationExceptionsShouldCarryTheProviderMessage
		assertThatThrownBy(() -> helper.getUserInfo("bad-token"))
			.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void getUserInfo_refusesWhenNoUserinfoEndpointIsConfigured() throws Exception {
		enqueueJson(jwks);
		SsoHelper helper = new SsoHelper(url("/token"), CLIENT_ID, CLIENT_SECRET, null, null,
			url("/certs"), "", url("/introspect"));

		assertThatThrownBy(() -> helper.getUserInfo("the-access-token"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Userinfo endpoint is not configured");
	}

	// ---------- introspection ----------

	@Test
	void callIntrospection_postsTheTokenWithTheClientCredentials() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		enqueueJson("{\"active\":true,\"exp\":1900000000}");

		Map<String, Object> result = helper.callIntrospection("the-token");

		assertThat(result).containsEntry("active", Boolean.TRUE);
		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath()).isEqualTo("/introspect");
		assertThat(request.getBody().readUtf8())
			.contains("token=the-token").contains("client_id=" + CLIENT_ID);
	}

	@Test
	void callIntrospection_returnsNullWhenTheEndpointIsNotConfigured() throws Exception {
		enqueueJson(jwks);
		SsoHelper helper = new SsoHelper(url("/token"), CLIENT_ID, CLIENT_SECRET, null, null,
			url("/certs"), url("/userinfo"), "");

		assertThat(helper.callIntrospection("the-token")).isNull();
	}

	@Test
	void callIntrospection_swallowsAProviderFailure() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		server.enqueue(new MockResponse().setResponseCode(500).setBody("{}"));

		// introspection is best-effort; a failure must not break the login
		assertThat(helper.callIntrospection("the-token")).isNull();
	}

	// ---------- token expiration ----------

	@Test
	void getTokenExpiration_readsItFromAJwtWithoutCallingTheProvider() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();

		Date expiration = helper.getTokenExpiration(signedToken("n"));

		assertThat(expiration).isAfter(new Date());
		// nothing beyond the JWKS fetch was needed
		assertThat(server.getRequestCount()).isEqualTo(1);
	}

	@Test
	void codeExchange_failsAuthenticationWhenTheProviderRejectsTheCode() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		server.enqueue(new MockResponse().setResponseCode(400)
			.setBody("{\"error\":\"invalid_grant\",\"error_description\":\"code expired\"}"));

		Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(
			() -> helper.codeExchange("stale-code", "https://app/callback", "nonce-1"));

		assertThat(thrown).isInstanceOf(AuthenticationException.class);
		// today the provider's own explanation survives only here
		assertThat(((AuthenticationException) thrown).getData())
			.anySatisfy(entry -> assertThat(String.valueOf(entry)).contains("code expired"));
	}

	/**
	 * TODO KNOWN BUG (not fixed): {@code org.cibseven.webapp.auth.exception.AuthenticationException}
	 * comes from the shared common-auth library and declares only
	 * {@code AuthenticationException(Object... data)}, never calling {@code super(message)}. Every
	 * authentication failure in the webclient therefore has a null {@code getMessage()} and a broken
	 * cause chain - the identity provider's own {@code error_description} is collected into
	 * {@code getData()} and nowhere else, so operators debugging a failed SSO login get an empty log
	 * line. Same shape as {@code ApplicationException}; see {@code ExceptionContractTest} in
	 * {@code cibseven-interfaces}. This test asserts what the log line should say; it fails until
	 * the library exception forwards its message.
	 */
	@Test
	@Disabled("KNOWN BUG: common-auth AuthenticationException never calls super(message), so every SSO failure logs an empty message")
	void authenticationExceptionsShouldCarryTheProviderMessage() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		server.enqueue(new MockResponse().setResponseCode(400)
			.setBody("{\"error\":\"invalid_grant\",\"error_description\":\"code expired\"}"));

		Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(
			() -> helper.codeExchange("stale-code", "https://app/callback", "nonce-1"));

		assertThat(thrown).isInstanceOf(AuthenticationException.class);
		assertThat(thrown.getMessage()).contains("code expired");
	}

	@Test
	void getTokenExpiration_fallsBackToIntrospectionForAnOpaqueToken() throws Exception {
		SsoHelper helper = helper();
		server.takeRequest();
		long expiresAt = System.currentTimeMillis() / 1000 + 600;
		enqueueJson("{\"active\":true,\"exp\":" + expiresAt + "}");

		Date expiration = helper.getTokenExpiration("opaque-token");

		assertThat(expiration).isNotNull().isAfter(new Date());
		assertThat(server.takeRequest().getPath()).isEqualTo("/introspect");
	}
}
