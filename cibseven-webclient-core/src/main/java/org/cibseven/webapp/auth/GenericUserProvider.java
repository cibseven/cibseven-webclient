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
import java.util.Collection;
import java.util.Optional;

import javax.crypto.SecretKey;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

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
		settings = new JwtTokenSettings(secret, validMinutes, prolongMinutes);
	}

	@Override
	public User getUserInfo(User user, String userId) {
		if (user.getId().compareTo(userId) == 0) {
			return user;
		} else {
			throw new AuthenticationException(userId);
		}
	}

	@Override
	public Object authenticateUser(HttpServletRequest request) {
		return authenticate(request);
	}

	@Override
	public User getSelfInfoJSessionId(String userId, String jSessionId, HttpServletRequest rq) {
		try {
			String url = cibsevenAdminUrl + "/auth/user/default";
			HttpHeaders headers = new HttpHeaders();
			headers.add("Cookie", "JSESSIONID=" + jSessionId);
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

			RestTemplate rest = new RestTemplate();
			SevenAdminAuth response = ((ResponseEntity<SevenAdminAuth>) rest.exchange(url, HttpMethod.GET,
					new HttpEntity<>(headers), SevenAdminAuth.class)).getBody();

			if (userId.equals(response.getUserId())) {
				CIBUser user = new CIBUser(userId);
				user.setAuthToken(createToken(getSettings(), true, false, user));
				return user;
			}
			return null;
		} catch (HttpStatusCodeException e) {
			throw new AuthenticationException(e);
		}
	}

	@Override
	public User deserialize(String json, String token) {
		try {
			ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
					false);
			CIBUser user = mapper.readValue(json, CIBUser.class);
			user.setAuthToken(token);
			return user;
		} catch (IllegalArgumentException x) {
			throw new AuthenticationException(json);
		} catch (IOException x) {
			throw new SystemException(x);
		}
	}

	@Override
	public String serialize(User user) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.addMixIn(CIBUser.class, UserSerialization.class);
			return mapper.writeValueAsString(user);
		} catch (JsonProcessingException x) {
			throw new SystemException(x);
		}
	}

	@Override
	public User verify(Claims userClaims) {
		return null;
	}

	public String verify(String token) {
		if (cibsevenWebclientUrl != null && !cibsevenWebclientUrl.isEmpty()) {
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token);
			// The following line fix the autorenew token
			headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
			RestTemplate template = new RestTemplate();
			try {
				return template
						.exchange(cibsevenWebclientUrl + "/auth", HttpMethod.GET, new HttpEntity(headers),
								CIBUser.class)
						.getBody().getAuthToken();
			} catch (HttpClientErrorException e) {
				ObjectMapper mapper = new ObjectMapper();
				try {
					ErrorMessage message = mapper.readValue(e.getResponseBodyAsString(), ErrorMessage.class);
					if (message.getType().equals("TokenExpiredException") && message.getParams().length > 0)
						return message.getParams()[0].toString();
					else
						return null;
				} catch (IOException ex) {
					log.debug("Webclient getSelfInfo response couldn't be parsed", ex);
					return null;
				}
			}
		} else
			return null;
	}

	public User parse(String token, TokenSettings settings) {
		try {
			SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(settings.getSecret()));
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			User user = deserialize((String) claims.get("user"), JwtUserProvider.BEARER_PREFIX + token);
			if ((boolean) claims.get("verify") && verify(token) == null)
				throw new AuthenticationException(token);
			return user;

		} catch (ExpiredJwtException x) {
			long ageMillis = System.currentTimeMillis() - x.getClaims().getExpiration().getTime();
			if ((boolean) x.getClaims().get("prolongable") && (ageMillis < settings.getProlong().toMillis())) {
				String newToken = verify(token);
				if (newToken != null)
					throw new TokenExpiredException(newToken);
			}
			throw new TokenExpiredException();

		} catch (JwtException x) {
			throw new AuthenticationException(token);
		}
	}

	@Override
	public User login(StandardLogin login, HttpServletRequest rq) {
		// This is needed when using security in engine-rest, we need to set the token
		// into our
		try {
			if (login != null) {
				CIBUser user = new CIBUser(login.getUsername());
				user.setAuthToken(createToken(getSettings(), true, false, user));
				return user;
			} else {
				return null;
			}

		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void logout(User arg0) {

	}

	@Override
	public StandardLogin createLoginParams() {
		return new StandardLogin();
	}

}
