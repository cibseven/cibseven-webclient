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
package org.cibseven.webapp.auth.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.JwtTokenSettings;
import org.cibseven.webapp.auth.User;
import org.cibseven.webapp.config.EngineRestProperties;
import org.cibseven.webapp.config.EngineRestSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Multi-engine token handling: which JWT secret a request is signed and verified with.
 * <p>
 * Getting this wrong is a security-relevant defect in both directions - a token minted for one
 * engine must not verify against another engine's secret, and a legitimate token must still
 * verify - so the secret-selection rules are pinned here.
 */
public class EngineTokenUtilsTest {

	private static final String DEFAULT_SECRET = "default-secret";
	private static final String ENGINE_SECRET = "engine-specific-secret";

	private JwtTokenSettings defaultSettings;

	@BeforeEach
	void setUp() {
		defaultSettings = new JwtTokenSettings(DEFAULT_SECRET, 60L, 30L);
	}

	private EngineRestProperties propertiesWith(EngineRestSource... sources) {
		EngineRestProperties properties = new EngineRestProperties();
		properties.setAdditionalEngineRest(List.of(sources));
		return properties;
	}

	private EngineRestSource source(String url, String path, String jwtSecret) {
		EngineRestSource source = new EngineRestSource();
		source.setUrl(url);
		source.setPath(path);
		source.setJwtSecret(jwtSecret);
		return source;
	}

	// ---------- setEngineFromRequest ----------

	@Test
	void setEngineFromRequest_copiesTheProcessEngineHeaderOntoTheUser() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("X-Process-Engine")).thenReturn("http://other|/engine-rest|remote");
		CIBUser user = new CIBUser("demo");

		EngineTokenUtils.setEngineFromRequest(user, request);

		assertThat(user.getEngine()).isEqualTo("http://other|/engine-rest|remote");
	}

	@Test
	void setEngineFromRequest_leavesTheEngineUnsetWhenTheHeaderIsAbsent() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("X-Process-Engine")).thenReturn(null);
		CIBUser user = new CIBUser("demo");

		EngineTokenUtils.setEngineFromRequest(user, request);

		assertThat(user.getEngine()).isNull();
	}

	@Test
	void setEngineFromRequest_ignoresAUserThatIsNotACibUser() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		User user = mock(User.class);

		// must not throw; there is simply nowhere to put the engine
		EngineTokenUtils.setEngineFromRequest(user, request);

		org.mockito.Mockito.verify(request, org.mockito.Mockito.never()).getHeader("X-Process-Engine");
	}

	// ---------- getSettingsForEngine ----------

	@Test
	void getSettingsForEngine_usesTheDefaultSecretForTheDefaultEngine() {
		assertThat(EngineTokenUtils.getSettingsForEngine(null, null, defaultSettings, 60L, 30L))
			.isSameAs(defaultSettings);
		assertThat(EngineTokenUtils.getSettingsForEngine("", null, defaultSettings, 60L, 30L))
			.isSameAs(defaultSettings);
	}

	@Test
	void getSettingsForEngine_usesTheDefaultSecretForALegacyEngineName() {
		// a bare name carries no url, so there is no additional-engine entry to match
		assertThat(EngineTokenUtils.getSettingsForEngine("production", null, defaultSettings, 60L, 30L))
			.isSameAs(defaultSettings);
	}

	@Test
	void getSettingsForEngine_usesTheDefaultSecretForAMalformedReference() {
		// two segments instead of three
		assertThat(EngineTokenUtils.getSettingsForEngine(
			"http://other|/engine-rest", null, defaultSettings, 60L, 30L)).isSameAs(defaultSettings);
	}

	@Test
	void getSettingsForEngine_picksTheEngineSpecificSecret() {
		EngineRestProperties properties =
			propertiesWith(source("http://other", "/engine-rest", ENGINE_SECRET));

		assertThat(EngineTokenUtils.getSettingsForEngine(
			"http://other|/engine-rest|remote", properties, defaultSettings, 60L, 30L).getSecret())
			.isEqualTo(ENGINE_SECRET);
	}

	@Test
	void getSettingsForEngine_carriesTheValidityDurationsOntoTheEngineSettings() {
		EngineRestProperties properties =
			propertiesWith(source("http://other", "/engine-rest", ENGINE_SECRET));

		var settings = EngineTokenUtils.getSettingsForEngine(
			"http://other|/engine-rest|remote", properties, defaultSettings, 15L, 5L);

		assertThat(settings.getValid()).isEqualTo(java.time.Duration.ofMinutes(15));
		assertThat(settings.getProlong()).isEqualTo(java.time.Duration.ofMinutes(5));
	}

	@Test
	void getSettingsForEngine_ignoresTrailingSlashesWhenMatchingTheUrl() {
		EngineRestProperties properties =
			propertiesWith(source("http://other/", "/engine-rest", ENGINE_SECRET));

		assertThat(EngineTokenUtils.getSettingsForEngine(
			"http://other|/engine-rest|remote", properties, defaultSettings, 60L, 30L).getSecret())
			.isEqualTo(ENGINE_SECRET);
	}

	@Test
	void getSettingsForEngine_defaultsAnEmptySourcePathToEngineRest() {
		EngineRestProperties properties = propertiesWith(source("http://other", "", ENGINE_SECRET));

		assertThat(EngineTokenUtils.getSettingsForEngine(
			"http://other|/engine-rest|remote", properties, defaultSettings, 60L, 30L).getSecret())
			.isEqualTo(ENGINE_SECRET);
	}

	@Test
	void getSettingsForEngine_addsTheLeadingSlashToBothPathsBeforeComparing() {
		EngineRestProperties properties = propertiesWith(source("http://other", "engine-rest", ENGINE_SECRET));

		assertThat(EngineTokenUtils.getSettingsForEngine(
			"http://other|engine-rest|remote", properties, defaultSettings, 60L, 30L).getSecret())
			.isEqualTo(ENGINE_SECRET);
	}

	@Test
	void getSettingsForEngine_fallsBackToTheDefaultWhenTheUrlDoesNotMatch() {
		EngineRestProperties properties =
			propertiesWith(source("http://somewhere-else", "/engine-rest", ENGINE_SECRET));

		// a token must never be verified with another engine's secret
		assertThat(EngineTokenUtils.getSettingsForEngine(
			"http://other|/engine-rest|remote", properties, defaultSettings, 60L, 30L))
			.isSameAs(defaultSettings);
	}

	@Test
	void getSettingsForEngine_fallsBackToTheDefaultWhenThePathDoesNotMatch() {
		EngineRestProperties properties =
			propertiesWith(source("http://other", "/other-rest", ENGINE_SECRET));

		assertThat(EngineTokenUtils.getSettingsForEngine(
			"http://other|/engine-rest|remote", properties, defaultSettings, 60L, 30L))
			.isSameAs(defaultSettings);
	}

	@Test
	void getSettingsForEngine_fallsBackToTheDefaultWhenTheMatchedSourceHasNoSecret() {
		EngineRestProperties properties = propertiesWith(source("http://other", "/engine-rest", null));

		assertThat(EngineTokenUtils.getSettingsForEngine(
			"http://other|/engine-rest|remote", properties, defaultSettings, 60L, 30L))
			.isSameAs(defaultSettings);
	}

	@Test
	void getSettingsForEngine_ignoresASourceWithoutAUrl() {
		EngineRestProperties properties = propertiesWith(source(null, "/engine-rest", ENGINE_SECRET));

		assertThat(EngineTokenUtils.getSettingsForEngine(
			"http://other|/engine-rest|remote", properties, defaultSettings, 60L, 30L))
			.isSameAs(defaultSettings);
	}

	@Test
	void getSettingsForEngine_toleratesPropertiesWithoutAdditionalEngines() {
		assertThat(EngineTokenUtils.getSettingsForEngine(
			"http://other|/engine-rest|remote", new EngineRestProperties(), defaultSettings, 60L, 30L))
			.isSameAs(defaultSettings);
	}

	// ---------- extractEngineFromToken ----------

	/** A JWT-shaped string whose payload carries the given user JSON, unsigned. */
	private String tokenWithUserClaim(String userJson) {
		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		String header = encoder.encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
		String payload = encoder.encodeToString(
			("{\"user\":" + userJson + "}").getBytes(StandardCharsets.UTF_8));
		return header + "." + payload + ".not-a-real-signature";
	}

	private static String quoted(String json) {
		return "\"" + json.replace("\"", "\\\"") + "\"";
	}

	@Test
	void extractEngineFromToken_readsTheEngineOutOfTheUserClaim() {
		String token = tokenWithUserClaim(quoted("{\"engine\":\"http://other|/engine-rest|remote\"}"));

		assertThat(EngineTokenUtils.extractEngineFromToken(token))
			.isEqualTo("http://other|/engine-rest|remote");
	}

	@Test
	void extractEngineFromToken_stripsTheBearerPrefix() {
		String token = tokenWithUserClaim(quoted("{\"engine\":\"remote\"}"));

		assertThat(EngineTokenUtils.extractEngineFromToken("Bearer " + token)).isEqualTo("remote");
	}

	@Test
	void extractEngineFromToken_doesNotVerifyTheSignature() {
		// it only peeks at the payload, so the secret is irrelevant at this point
		String token = tokenWithUserClaim(quoted("{\"engine\":\"remote\"}"));

		assertThat(EngineTokenUtils.extractEngineFromToken(token)).isEqualTo("remote");
	}

	@Test
	void extractEngineFromToken_isNullWhenTheUserCarriesNoEngine() {
		String token = tokenWithUserClaim(quoted("{\"userID\":\"demo\"}"));

		assertThat(EngineTokenUtils.extractEngineFromToken(token)).isNull();
	}

	@Test
	void extractEngineFromToken_isNullWhenThereIsNoUserClaim() {
		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		String token = encoder.encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8))
			+ "." + encoder.encodeToString("{\"sub\":\"demo\"}".getBytes(StandardCharsets.UTF_8))
			+ ".sig";

		assertThat(EngineTokenUtils.extractEngineFromToken(token)).isNull();
	}

	@Test
	void extractEngineFromToken_isNullForSomethingThatIsNotAJwt() {
		assertThat(EngineTokenUtils.extractEngineFromToken("just-an-opaque-token")).isNull();
	}

	@Test
	void extractEngineFromToken_isNullForAnUndecodablePayload() {
		// three segments, but the middle one is not base64url JSON
		assertThat(EngineTokenUtils.extractEngineFromToken("a.!!!.c")).isNull();
	}

	// ---------- getEffectiveSettingsForToken ----------

	@Test
	void getEffectiveSettingsForToken_selectsTheSecretOfTheEngineNamedInTheToken() {
		EngineRestProperties properties =
			propertiesWith(source("http://other", "/engine-rest", ENGINE_SECRET));
		String token = tokenWithUserClaim(quoted("{\"engine\":\"http://other|/engine-rest|remote\"}"));

		assertThat(EngineTokenUtils.getEffectiveSettingsForToken(
			token, properties, defaultSettings, 60L, 30L).getSecret()).isEqualTo(ENGINE_SECRET);
	}

	@Test
	void getEffectiveSettingsForToken_fallsBackToTheDefaultForAnOpaqueToken() {
		EngineRestProperties properties =
			propertiesWith(source("http://other", "/engine-rest", ENGINE_SECRET));

		assertThat(EngineTokenUtils.getEffectiveSettingsForToken(
			"opaque", properties, defaultSettings, 60L, 30L)).isSameAs(defaultSettings);
	}
}
