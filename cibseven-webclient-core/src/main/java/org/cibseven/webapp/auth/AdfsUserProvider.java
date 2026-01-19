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

import java.util.Base64;

import javax.crypto.SecretKey;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.auth.providers.JwtUserProvider;
import org.cibseven.webapp.auth.sso.SSOLogin;
import org.cibseven.webapp.auth.sso.SSOUser;
import org.cibseven.webapp.auth.sso.SsoHelper;
import org.cibseven.webapp.auth.sso.TokenResponse;
import org.springframework.beans.factory.annotation.Value;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * @deprecated Use OAuth2UserProvider instead
 */
@Slf4j
@Deprecated
public class AdfsUserProvider extends OAuth2UserProvider {
	
	@Value("${cibseven.webclient.sso.domain:}") String domain;
	@Value("${cibseven.webclient.sso.infoInIdToken: false}") boolean infoInIdToken;
	@Value("${cibseven.webclient.sso.technicalUserId}") String technicalUserId;

	@PostConstruct
	@Override
	public void init() {
		settings = new JwtTokenSettings(secret, validMinutes, prolongMinutes);
		ssoHelper = new SsoHelper(tokenEndpoint, clientId, clientSecret, certEndpoint, null, null);
		checkKey();
		SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(settings.getSecret()));
		flowParser = Jwts.parser().verifyWith(key).build();
	}

	@Override
	public User login(SSOLogin params, HttpServletRequest rq) {
		TokenResponse tokens;
		if (params.getUsername() == null)
			tokens = getSsoHelper().codeExchange(params.getCode(), params.getRedirectUrl(), params.getNonce(), false, true);
		else {
			String username = params.getUsername();
			if (!username.contains("@") && !username.contains("\\"))
				username = username + "@" + domain;
			tokens = getSsoHelper().passwordLogin(username, params.getPassword());
		}
		
		Claims userClaims = infoInIdToken ? tokens.getIdClaims() : tokens.getAccessClaims();
		SSOUser user = new SSOUser(userClaims.get(userIdProperty, String.class));
		user.setDisplayName(userClaims.get(userNameProperty, String.class));
		user.setRefreshToken(tokens.getRefresh_token());
		
		// Set engine from request header
		setEngineFromRequest(user, rq);
		
		user.setAuthToken(createToken(getSettings(), true, false, user));
		user.setRefreshToken(null);
		return user;
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
		} catch (JwtException x) {
			throw new AuthenticationException(token);
		} catch (IllegalArgumentException e) {
			Claims claims = getSsoHelper().getKeyResolver().checkToken(token);
			SSOUser user = new SSOUser(technicalUserId);
			user.setDisplayName(claims.get("appid", String.class));
			user.setAuthToken(createToken(settings, false, false, user));
			return user;
		}
	}
}