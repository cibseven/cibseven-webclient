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

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKey;

import jakarta.servlet.http.HttpServletRequest;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.auth.providers.JwtUserProvider;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.exception.ErrorMessage;
import org.cibseven.webapp.exception.SystemException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenericUserProvider extends BaseUserProvider<StandardLogin> implements InitializingBean {

	@Value("${cibseven.webclient.authentication.jwtSecret:}")
	String secret;
	@Value("${cibseven.webclient.authentication.tokenValidMinutes:60}")
	long validMinutes;
	@Value("${cibseven.webclient.authentication.tokenProlongMinutes:1440}")
	long prolongMinutes;

	@Value("${cibseven.webclient.admin.url:}")
	String cibsevenAdminUrl;
	@Value("${cibseven.webclient.url:}")
	String cibsevenWebclientUrl;

	public void afterPropertiesSet() {
		log.info("Initializing GenericUserProvider with validMinutes={}, prolongMinutes={}", validMinutes, prolongMinutes);
		settings = new JwtTokenSettings(secret, validMinutes, prolongMinutes);
	}

	@Override
	public User getUserInfo(User user, String userId) {
		log.debug("Getting user info for userId: {}", userId);
		if (user.getId().compareTo(userId) == 0) {
			log.debug("User info retrieved successfully for userId: {}", userId);
			return user;
		} else {
			log.warn("Authentication failed: user {} attempted to access info for user {}", user.getId(), userId);
			throw new AuthenticationException(userId);
		}
	}

	@Override
	public Object authenticateUser(HttpServletRequest request) {
		log.debug("Authenticating user from request");
		Object result = authenticate(request);
		log.debug("User authentication completed");
		return result;
	}

	@Override
	public User getSelfInfoJSessionId(String userId, String jSessionId, HttpServletRequest rq) {
		log.debug("Getting self info for userId: {} with JSessionId", userId);
		try {
			String url = cibsevenAdminUrl + "/auth/user/default";
			log.debug("Making request to: {}", url);
			HttpHeaders headers = new HttpHeaders();
			headers.add("Cookie", "JSESSIONID=" + jSessionId);
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

			RestTemplate rest = new RestTemplate();
			SevenAdminAuth response = ((ResponseEntity<SevenAdminAuth>) rest.exchange(url, HttpMethod.GET,
					new HttpEntity<>(headers), SevenAdminAuth.class)).getBody();

			if (userId.equals(response.getUserId())) {
				log.debug("User ID verified successfully: {}", userId);
				CIBUser user = new CIBUser(userId);
				user.setAuthToken(createToken(getSettings(), true, false, user));
				log.info("User authenticated successfully with JSessionId: {}", userId);
				return user;
			}
			log.warn("User ID mismatch. Expected: {}, Actual: {}", userId, response.getUserId());
			return null;
		} catch (HttpStatusCodeException e) {
			log.error("Authentication failed for user: {}. Status code: {}", userId, e.getStatusCode(), e);
			throw new AuthenticationException(e);
		}
	}

	@Override
	public User deserialize(String json, String token) {
		log.debug("Deserializing user from JSON");
		try {
			ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
					false);
			CIBUser user = mapper.readValue(json, CIBUser.class);
			user.setAuthToken(token);
			log.debug("User deserialized successfully: {}", user.getId());
			return user;
		} catch (IllegalArgumentException x) {
			log.error("Failed to deserialize user due to illegal argument", x);
			throw new AuthenticationException(json);
		} catch (IOException x) {
			log.error("Failed to deserialize user due to IO exception", x);
			throw new SystemException(x);
		}
	}

	@Override
	public String serialize(User user) {
		log.debug("Serializing user to JSON: {}", user.getId());
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.addMixIn(CIBUser.class, UserSerialization.class);
			String result = mapper.writeValueAsString(user);
			log.debug("User serialized successfully");
			return result;
		} catch (JsonProcessingException x) {
			log.error("Failed to serialize user: {}", user.getId(), x);
			throw new SystemException(x);
		}
	}

	@Override
	public User verify(Claims userClaims) {
		return null;
	}

	public String verify(String token) {
		log.debug("Verifying token");
		if (cibsevenWebclientUrl != null && !cibsevenWebclientUrl.isEmpty()) {
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token);
			// The following line fix the autorenew token
			headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
			RestTemplate template = new RestTemplate();
			String url = cibsevenWebclientUrl + "/auth";
			log.debug("Making request to: {}", url);
			try {
				String authToken = template
						.exchange(url, HttpMethod.GET, new HttpEntity(headers),
								CIBUser.class)
						.getBody().getAuthToken();
				log.debug("Token verified successfully");
				return authToken;
			} catch (HttpClientErrorException e) {
				log.warn("Error verifying token: {}", e.getStatusCode());
				ObjectMapper mapper = new ObjectMapper();
				try {
					ErrorMessage message = mapper.readValue(e.getResponseBodyAsString(), ErrorMessage.class);
					if (message.getType().equals("TokenExpiredException") && message.getParams().length > 0) {
						log.info("Token expired, new token provided");
						return message.getParams()[0].toString();
					} else {
						log.warn("Token verification failed: {}", message.getType());
						return null;
					}
				} catch (IOException ex) {
					log.debug("Flow webclient getSelfInfo response couldn't be parsed", ex);
					return null;
				}
			}
		} else {
			log.warn("Flow webclient URL is not configured, token verification skipped");
			return null;
		}
	}

	public User parse(String token, TokenSettings settings) {
		log.debug("Parsing token");
		try {
			SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(settings.getSecret()));
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			log.debug("Token claims parsed successfully");
			User user = deserialize((String) claims.get("user"), JwtUserProvider.BEARER_PREFIX + token);

			if ((boolean) claims.get("verify")) {
				log.debug("Token requires verification");
				if (verify(token) == null) {
					log.warn("Token verification failed");
					throw new AuthenticationException(token);
				}
			}
			log.info("Token parsed successfully for user: {}", user.getId());
			return user;

		} catch (ExpiredJwtException x) {
			log.warn("Token expired for subject: {}", x.getClaims().getSubject());
			long ageMillis = System.currentTimeMillis() - x.getClaims().getExpiration().getTime();
			if ((boolean) x.getClaims().get("prolongable") && (ageMillis < settings.getProlong().toMillis())) {
				log.debug("Token is prolongable, attempting to get new token");
				String newToken = verify(token);
				if (newToken != null) {
					log.info("New token obtained for expired token");
					throw new TokenExpiredException(newToken);
				}
			}
			log.warn("Token expired and cannot be prolonged");
			throw new TokenExpiredException();

		} catch (JwtException x) {
			log.error("JWT exception while parsing token", x);
			throw new AuthenticationException(token);
		}
	}

	@Override
	public User login(StandardLogin login, HttpServletRequest rq) {
		// This is needed when using security in engine-rest, we need to set the token
		// into our
		log.debug("Login attempt");
		try {
			if (login != null) {
				log.debug("Login attempt for user: {}", login.getUsername());
				CIBUser user = new CIBUser(login.getUsername());
				String token = createToken(getSettings(), true, false, user);
				token = verify(token);
				if (token == null) {
					log.warn("Authentication failed for user: {}", login.getUsername());
					throw new AuthenticationException(login.getUsername());
				} else {
					log.info("User authenticated successfully: {}", user.getId());
					user.setAuthToken(token);
					return user;
				}
			} else {
				log.warn("Login attempt with null login parameters");
				return null;
			}
		} catch (Exception e) {
			log.error("Exception during login process", e);
			return null;
		}
	}

	@Override
	public void logout(User user) {
		if (user != null) {
			log.info("User logged out: {}", user.getId());
		} else {
			log.warn("Logout called with null user");
		}
	}

	@Override
	public StandardLogin createLoginParams() {
		log.debug("Creating login parameters");
		return new StandardLogin();
	}

}
