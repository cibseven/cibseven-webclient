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
import java.util.Collection;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KeycloakUserProvider extends BaseUserProvider<SSOLogin> {
	
	@Value("${sso.endpoints.token}") String tokenEndpoint;
	@Value("${sso.endpoints.jwks}") String certEndpoint;
	@Value("${sso.endpoints.user}") String userEndpoint;
	@Value("${sso.clientId}") String clientId;
	@Value("${sso.clientSecret}") String clientSecret;
	@Value("${sso.userIdProperty}") String userIdProperty;
	@Value("${sso.userNameProperty}") String userNameProperty;
	@Value("${authentication.jwtSecret}") String secret;
	@Value("${authentication.tokenValidMinutes}") long validMinutes;
	@Value("${authentication.tokenProlongMinutes}") long prolongMinutes;
	
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
		ssoHelper = new SsoHelper(tokenEndpoint, clientId, clientSecret, certEndpoint, userEndpoint);
		SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(settings.getSecret()));
		flowParser = Jwts.parser().verifyWith(key).build();
	}

	@Override
	public User login(SSOLogin params, HttpServletRequest rq) {
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
	public Collection<CIBUser> getUsers(@NotNull User user, Optional<String> str) {
		
		MultiValueMap<String, String> rqParams = new LinkedMultiValueMap<>();
		rqParams.add("client_id", clientId);
		rqParams.add("client_secret", clientSecret);
		rqParams.add("grant_type", "client_credentials");

		TokenResponse tokens = null;
		try {
			HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(rqParams, formUrlEncodedHeader);
			RestTemplate template = new RestTemplate();
			tokens = template.postForObject(tokenEndpoint, tokenRequest, TokenResponse.class);
			if (tokens != null) log.debug(tokens.getId_token());
			if (tokens != null) log.debug(tokens.getAccess_token());
		} catch (RestClientResponseException e) {
			throw new SystemException("Couldn't authenticate client user " + e.getResponseBodyAsString());
		}
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Authorization", "Bearer " + tokens.getAccess_token());
			HttpEntity tokenRequest = new HttpEntity(headers);
			RestTemplate template = new RestTemplate();
			List<LinkedHashMap<String, ?>> users = template.exchange(userEndpoint, HttpMethod.GET, tokenRequest, new ParameterizedTypeReference<List<LinkedHashMap<String, ?>>>() {}).getBody();
			System.out.println(users.get(0));
			return users.stream().map(map -> {
				CIBUser foundUser = new CIBUser(map.get("username").toString());
				foundUser.setDisplayName(map.get("firstName").toString() + " " + map.get("lastName").toString());
				return foundUser;
			}).collect(Collectors.toList());
		} catch (RestClientResponseException e) {
			throw new SystemException("Couldn't get users " + e.getResponseBodyAsString());
		}
	}

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
			Map<String, String> userInfo = ssoHelper.getUserInfo(token);
			SSOUser user = new SSOUser();
			user.setDisplayName(userInfo.get(userNameProperty));
	        user.setUserID(userInfo.get(userIdProperty));
	        
			return user;
		} catch (JwtException x) {
			throw new AuthenticationException(token);
		}
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