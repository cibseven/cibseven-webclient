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
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.crypto.SecretKey;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.auth.providers.JwtUserProvider;
import org.cibseven.webapp.auth.utils.EngineTokenUtils;
import org.cibseven.webapp.auth.sso.SSOLogin;
import org.cibseven.webapp.auth.sso.SSOUser;
import org.cibseven.webapp.auth.sso.SsoHelper;
import org.cibseven.webapp.auth.sso.TokenCache;
import org.cibseven.webapp.auth.sso.TokenResponse;
import org.cibseven.webapp.exception.SystemException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
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
	@Value("${cibseven.webclient.sso.accessTokenToEngineRest:false}") boolean forwardToken;
	@Value("${cibseven.webclient.authentication.jwtSecret}") String secret;
	
	@Getter private SsoHelper ssoHelper;
	@Getter private JwtTokenSettings settings;
	HttpHeaders formUrlEncodedHeader = new HttpHeaders();
	{
		formUrlEncodedHeader.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
	}
	JwtParser flowParser;
	private final ConcurrentMap<String, TokenCache> cachedAccessToken = new ConcurrentHashMap<>();
	private ScheduledExecutorService scheduler;
	
	@PostConstruct
	public void init() {
		settings = new JwtTokenSettings(secret, validMinutes, prolongMinutes);
		ssoHelper = new SsoHelper(tokenEndpoint, clientId, clientSecret, certEndpoint, userEndpoint, introspectionEndpoint);
		checkKey();
		SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(settings.getSecret()));
		flowParser = Jwts.parser().verifyWith(key).build();
		
		if (forwardToken) {
			// Schedule cleanup of expired tokens every 10 minutes
			scheduler = Executors.newSingleThreadScheduledExecutor();
			scheduler.scheduleAtFixedRate(this::cleanup, 10, 10, java.util.concurrent.TimeUnit.MINUTES);
		}
	}
	
	@PreDestroy
	public void destroy() {
		if (scheduler != null) scheduler.shutdownNow();
	}

	private void cleanup() {
		long now = System.currentTimeMillis();
		cachedAccessToken.entrySet().removeIf(e -> {
			TokenCache entry = e.getValue();
			Date expiration = entry.getExpiration();
			if (expiration == null || (expiration.before(new Date(now)) && !entry.isCurrentlyFetched())) {
				return true;
			}
			return false;
		});
	}
	
	@Override
	public String getEngineRestToken(CIBUser user) {
		if (forwardToken) {
			SSOUser oauthUser = (SSOUser) user;
			String refreshToken = oauthUser.getRefreshToken();
			String cacheKey = user.getId() + refreshToken;
			
			TokenCache entry = cachedAccessToken.computeIfAbsent(cacheKey, k -> new TokenCache(null, null, true));

			synchronized (entry) {
				// Rolling refresh tokens are NOT supported!
				if (entry.getAccessToken() != null) {
					try {
						Date expiration = entry.getExpiration();
						if (expiration != null && expiration.after(new Date(System.currentTimeMillis() + 5000)))
							return JwtUserProvider.BEARER_PREFIX + entry.getAccessToken();
					} catch (JwtException e) {
						 // remove invalid token from cache
						entry.setAccessToken(null);
						entry.setExpiration(null);
					}
				}
				entry.setCurrentlyFetched(true);

				try {
					TokenResponse tokens = ssoHelper.refreshToken(oauthUser.getRefreshToken());
					Date expiration = ssoHelper.getTokenExpiration(tokens.getAccess_token());
					entry.setAccessToken(tokens.getAccess_token());
					entry.setExpiration(expiration);
					return JwtUserProvider.BEARER_PREFIX + tokens.getAccess_token();
				} finally {
					entry.setCurrentlyFetched(false);
				}
			}
		}
		return super.getEngineRestToken(user);
	}

	@Override
	public User login(SSOLogin params, HttpServletRequest rq) {
		if (params.getAuthToken() != null && !params.getAuthToken().isEmpty()) {
			// Get the user from the access token
			SSOUser user = getUserFromAccessToken(params.getAuthToken());
			
			// Set engine from request header
			EngineTokenUtils.setEngineFromRequest(user, rq);
			
			// Use SsoHelper to extract token expiration
			Date expiration = ssoHelper.getTokenExpiration(params.getAuthToken());
			
			// Get the appropriate token settings for this engine
			TokenSettings tokenSettings = EngineTokenUtils.getSettingsForEngine(
				user.getEngine(), engineRestProperties, getSettings(), validMinutes, prolongMinutes);
			if (expiration != null) {
				// Create a custom token settings with the expiration from the third-party token
				long tokenValidMinutes = (expiration.getTime() - System.currentTimeMillis()) / 60000; // Convert to minutes
				if (tokenValidMinutes > 0) {
					tokenSettings = new JwtTokenSettings(tokenSettings.getSecret(), tokenValidMinutes, 0);
				}
			}
			
			user.setAuthToken(createToken(tokenSettings, false, false, user));
			return user;
		}
		TokenResponse tokens = ssoHelper.codeExchange(params.getCode(), params.getRedirectUrl(), params.getNonce(), false, true);
		
		SSOUser user = new SSOUser(tokens.getIdClaims().get(userIdProperty, String.class));
		user.setDisplayName(tokens.getIdClaims().get(userNameProperty, String.class));
		user.setRefreshToken(tokens.getRefresh_token());
		
		// Set engine from request header
		EngineTokenUtils.setEngineFromRequest(user, rq);
		
		// Get the appropriate token settings for this engine
		TokenSettings tokenSettings = EngineTokenUtils.getSettingsForEngine(
			user.getEngine(), engineRestProperties, getSettings(), validMinutes, prolongMinutes);
		user.setAuthToken(createToken(tokenSettings, true, false, user));
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
			// Get effective settings based on engine from token
			TokenSettings effectiveSettings = EngineTokenUtils.getEffectiveSettingsForToken(
				token, engineRestProperties, settings, validMinutes, prolongMinutes);
			SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(effectiveSettings.getSecret()));
			JwtParser parser = Jwts.parser().verifyWith(key).build();
			
			Claims claims = parser.parseSignedClaims(token).getPayload();
			User user = deserialize((String) claims.get("user"), JwtUserProvider.BEARER_PREFIX + token);
			if ((boolean) claims.get("verify") && verify(claims) == null)
				throw new AuthenticationException(token);
			return user;
		} catch (ExpiredJwtException x) {
			// Get effective settings based on engine from token
			TokenSettings effectiveSettings = EngineTokenUtils.getEffectiveSettingsForToken(
				token, engineRestProperties, settings, validMinutes, prolongMinutes);
			
			long ageMillis = System.currentTimeMillis() - x.getClaims().getExpiration().getTime();
			if ((boolean) x.getClaims().get("prolongable") && (ageMillis < effectiveSettings.getProlong().toMillis())) {
				User user = verify(x.getClaims());
				if (user != null)
					throw new TokenExpiredException(createToken(effectiveSettings, true, false, user));				
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