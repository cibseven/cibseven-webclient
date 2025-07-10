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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.auth.providers.JwtUserProvider;
import org.cibseven.webapp.auth.sso.SSOLogin;
import org.cibseven.webapp.auth.sso.SSOUser;
import org.cibseven.webapp.auth.sso.SsoHelper;
import org.cibseven.webapp.auth.sso.TokenResponse;
import org.cibseven.webapp.exception.SystemException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KeycloakUserProvider extends BaseUserProvider<SSOLogin> {
	
	@Value("${cibseven.webclient.sso.endpoints.token}") String tokenEndpoint;
	@Value("${cibseven.webclient.sso.endpoints.jwks}") String certEndpoint;
	@Value("${cibseven.webclient.sso.endpoints.user}") String userEndpoint;
	@Value("${cibseven.webclient.sso.endpoints.introspection:}") String introspectionEndpoint;
	@Value("${cibseven.webclient.sso.clientId}") String clientId;
	@Value("${cibseven.webclient.sso.clientSecret}") String clientSecret;
	@Value("${cibseven.webclient.sso.userIdProperty}") String userIdProperty;
	@Value("${cibseven.webclient.sso.userNameProperty}") String userNameProperty;
	@Value("${cibseven.webclient.authentication.jwtSecret}") String secret;
	@Value("${cibseven.webclient.authentication.tokenValidMinutes}") long validMinutes;
	@Value("${cibseven.webclient.authentication.tokenProlongMinutes}") long prolongMinutes;
	
	@Getter private SsoHelper ssoHelper;
	@Getter private JwtTokenSettings settings;
	HttpHeaders formUrlEncodedHeader = new HttpHeaders();
	{
		formUrlEncodedHeader.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
	}
	JwtParser flowParser;
	
	@PostConstruct
	public void init() {
		settings = new JwtTokenSettings(secret, validMinutes, prolongMinutes);
		ssoHelper = new SsoHelper(tokenEndpoint, clientId, clientSecret, certEndpoint, userEndpoint, introspectionEndpoint);
		SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(settings.getSecret()));
		flowParser = Jwts.parser().verifyWith(key).build();
	}

	@Override
	public User login(SSOLogin params, HttpServletRequest rq) {
		if (params.getAuthToken() != null && !params.getAuthToken().isEmpty()) {
			// Get the user from the access token
			SSOUser user = getUserFromAccessToken(params.getAuthToken());
			
			// Use SsoHelper to extract token expiration
			Date expiration = ssoHelper.getTokenExpiration(params.getAuthToken());
			
			// Create a new token with the expiration from the third-party token if available
			JwtTokenSettings tokenSettings = getSettings();
			if (expiration != null) {
				// Create a custom token settings with the expiration from the third-party token
				long validMinutes = (expiration.getTime() - System.currentTimeMillis()) / 60000; // Convert to minutes
				if (validMinutes > 0) {
					tokenSettings = new JwtTokenSettings(tokenSettings.getSecret(), validMinutes, 0);
				}
			}
			
			user.setAuthToken(createToken(tokenSettings, false, false, user));
			return user;
		}
		TokenResponse tokens = ssoHelper.codeExchange(params.getCode(), params.getRedirectUrl(), params.getNonce(), false, true);
		
		SSOUser user = new SSOUser(tokens.getIdClaims().get(userIdProperty, String.class));
		user.setDisplayName(tokens.getIdClaims().get(userNameProperty, String.class));
		user.setRefreshToken(tokens.getRefresh_token());
		user.setAuthToken(createToken(getSettings(), true, false, user));
		user.setRefreshToken(null);
		return user;
	}

	@Override
	public User getUserInfo(User user, String userId) {
		if (user.getId().equals(userId)) {
			((SSOUser) user).setRefreshToken(null);
			return user;
		}
		else
			throw new SystemException("Not implemented");
	}

	@Override
	public User deserialize(String json, String token) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.addMixIn(SSOUser.class, org.cibseven.webapp.auth.BaseUserProvider.UserSerialization.class);
			SSOUser user = mapper.readValue(json, SSOUser.class);
			user.setAuthToken(token);
			return user;
		} catch (IllegalArgumentException x) { // for example doXigate token used with doXisafe
			throw new AuthenticationException(json);
		} catch (IOException x) {
			throw new SystemException(x);
		}
	}

	@Override
	public String serialize(User user) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.addMixIn(SSOUser.class, UserSerialization.class);
			return mapper.writeValueAsString(user);
		} catch (JsonProcessingException x) {
			throw new SystemException(x);
		}
	}

	@Override
	public User verify(Claims claims) {
		SSOUser user = (SSOUser) deserialize(claims.get("user", String.class), "");
		TokenResponse tokens = ssoHelper.refreshToken(user.getRefreshToken());
		user.setRefreshToken(tokens.getRefresh_token());
		return user;
	}

	@Override
	public void logout(User user) {	}

	@Override
	public Object authenticateUser(HttpServletRequest request) {
		return authenticate(request);
	}
	
	@Override
	public User parse(String token, TokenSettings settings) {
		try {			
			Claims claims = flowParser.parseSignedClaims(token).getPayload();
			User user = deserialize((String) claims.get("user"), JwtUserProvider.BEARER_PREFIX + token);
			if ((boolean) claims.get("verify") && verify(claims) == null)
				throw new AuthenticationException(token);
			return user;
		} catch (ExpiredJwtException x) {
			long ageMillis = System.currentTimeMillis() - x.getClaims().getExpiration().getTime();
			if ((boolean) x.getClaims().get("prolongable") && (ageMillis < settings.getProlong().toMillis())) {
				User user = verify(x.getClaims());
				if (user != null)
					throw new TokenExpiredException(createToken(settings, true, false, user));				
			}
			throw new TokenExpiredException();			
		} catch (IllegalArgumentException | InvalidKeyException | MalformedJwtException e) { //token received from third-party isn't ours, get the userInfo from the defined endpoint on the config file
			User user = getUserFromAccessToken(token);
	        
			return user;
		} catch (JwtException x) {
			throw new AuthenticationException(token);
		}
	}

	private SSOUser getUserFromAccessToken(String token) {
		Map<String, String> userInfo = ssoHelper.getUserInfo(token);
		SSOUser user = new SSOUser();
		if (userInfo == null) {
			Map<String, Object> introspectionResult = ssoHelper.callIntrospection(token);
			//Use subject or username as userId as we get no userInfo
			if (introspectionResult != null) {
				user.setDisplayName((String) introspectionResult.getOrDefault("sub", introspectionResult.get("username")));
				user.setUserID((String) introspectionResult.getOrDefault("sub", introspectionResult.get("username")));
			}
		} else {
			user.setDisplayName(userInfo.get(userNameProperty));
			user.setUserID(userInfo.get(userIdProperty));
		}
		if (user.getUserID() == null || user.getDisplayName() == null) {
			throw new AuthenticationException("Invalid token");
		}
		return user;
	}

	@Override
	public SSOLogin createLoginParams() {
		return new SSOLogin();
	}

	@Override
	public User getSelfInfoJSessionId(String userId, String jSessionId, HttpServletRequest rq) {
		return null;
	}
}