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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.auth.providers.JwtUserProvider;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.SevenUser;
import org.cibseven.webapp.rest.model.SevenVerifyUser;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

public class SevenUserProvider extends BaseUserProvider<StandardLogin> implements InitializingBean {
	
	@Value("${cibseven.webclient.authentication.jwtSecret:}") String secret;	
	@Value("${cibseven.webclient.authentication.tokenValidMinutes:60}") long validMinutes;	
	@Value("${cibseven.webclient.authentication.tokenProlongMinutes:1440}") long prolongMinutes;
	
	@Value("${cibseven.webclient.engineRest.url:./}") String cibsevenUrl;
	
	@Autowired BpmProvider provider;
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		settings = new JwtTokenSettings(secret, validMinutes, prolongMinutes);
		if (provider instanceof SevenProvider)
			sevenProvider = (SevenProvider) provider;
		else throw new SystemException("SevenUserProvider expects a SevenProvider");
	}
	
	@Override
	public CIBUser login(StandardLogin login, HttpServletRequest rq) {	
		try {
			CIBUser user =  new CIBUser(login.getUsername());
			SevenVerifyUser sevenVerifyUser = sevenProvider.verifyUser(user.getId(), login.getPassword(), user);
			
			if (sevenVerifyUser.isAuthenticated()) {
			  // Token is needed for the next request (/user/xxx/profile)
			  user.setAuthToken(createToken(getSettings(), true, false, user));
				SevenUser cUser = sevenProvider.getUserProfile(user.getId(), user);
				user.setUserID(cUser.getId());
				user.setDisplayName(cUser.getFirstName() + " " + cUser.getLastName());
				// Token is created for the second time to include the display name
				user.setAuthToken(createToken(getSettings(), true, false, user));
				return user;	
			}
			else {
				throw new AuthenticationException(login.getUsername());		
			}
			
		} catch(Exception e) {
			throw new AuthenticationException(login.getUsername());	
		}
	}
	
	@Override
	public void logout(User user) { 
		
	}
	
	@Override
	public Collection<CIBUser> getUsers(User user, Optional<String> filter) {
		Collection<SevenUser> sevenUsers = sevenProvider.fetchUsers((CIBUser) user);
		
		Collection<CIBUser> users = new ArrayList<>();
		
		for (SevenUser sevenUser: sevenUsers) {
		    CIBUser foundUser = new CIBUser(sevenUser.getId());
		    foundUser.setDisplayName(sevenUser.getFirstName() + " " + sevenUser.getLastName());
		    users.add(foundUser);
		}
		return users;
	}	

	@Override
	public User getUserInfo(User user, String userId) {
		if (user.getId().equals(userId)) {
			return user;
		}
		else {
			throw new AuthenticationException(userId);
		}
	}
	
	@Override
	public Object authenticateUser(HttpServletRequest request) {
		return authenticate(request);
	}

	@Override
	public User getSelfInfoJSessionId(String userId, String jSessionId, HttpServletRequest rq) {
		return null;
	}
	
	@Override	
	public User deserialize(String json, String token) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.addMixIn(CIBUser.class, UserSerialization.class);
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
        try {			
        	return (CIBUser) deserialize(userClaims.get("user").toString(), null);
			//TODO? Verify User if wants to use in prod.
		} catch(Exception e) {
			throw new AuthenticationException(userClaims.get("user").toString());
		}
        
	}
	
	public User parse(String token, TokenSettings settings) {
		try {
			SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(settings.getSecret()));
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
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
		}
	}
	
	@Override
	public StandardLogin createLoginParams() {
		return new StandardLogin();
	}
}
