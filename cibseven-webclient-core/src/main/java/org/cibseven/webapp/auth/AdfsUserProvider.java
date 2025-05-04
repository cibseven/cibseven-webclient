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
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.auth.providers.JwtUserProvider;
import org.cibseven.webapp.auth.sso.SSOLogin;
import org.cibseven.webapp.auth.sso.SSOUser;
import org.cibseven.webapp.auth.sso.SsoHelper;
import org.cibseven.webapp.auth.sso.TokenResponse;
import org.cibseven.webapp.exception.SystemException;
import org.eclipse.microprofile.config.ConfigProvider;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
public class AdfsUserProvider extends BaseUserProvider<SSOLogin> {
	
	@Value("${sso.endpoints.token}") String tokenEndpoint;
	@Value("${sso.endpoints.jwks}") String certEndpoint;
	@Value("${sso.domain:}") String domain;
	@Value("${sso.clientId}") String clientId;
	@Value("${sso.clientSecret}") String clientSecret;
	@Value("${sso.userIdProperty}") String userIdProperty;
	@Value("${sso.userNameProperty}") String userNameProperty;
	@Value("${sso.infoInIdToken: false}") boolean infoInIdToken;
	@Value("${sso.technicalUserId}") String technicalUserId;
	
	@Value("${authentication.jwtSecret}") String secret;
	@Value("${authentication.tokenValidMinutes}") long validMinutes;
	@Value("${authentication.tokenProlongMinutes}") long prolongMinutes;
	
	@Value("${ldap.url}") String serverURL;
	@Value("${ldap.user}") String ldapUser;
	@Value("${ldap.password}") String ldapPassword;
	@Value("${ldap.folder}") String ldapFolder;
	@Value("${ldap.countLimit:400}") int ldapCountLimit;
	@Value("#{'${ldap.attributes.filters:samAccountName;name}'.split(';\\s*')}") List<String> ldapAttributesFilters;
	@Value("${ldap.userNameAttribute}") String ldapNameAttribute;
	@Value("${ldap.userDisplayNameAttribute}") String ldapDisplayNameAttribute;
	@Value("${ldap.followReferrals}") String ldapFollowReferrals;
	
	@Getter JwtTokenSettings settings;
	SsoHelper ssoHelper;
	JwtParser flowParser;
	
	@PostConstruct
	public void init() {
		String secret = ConfigProvider.getConfig().getValue("MP_JWT_SECRET", String.class);
		settings = new JwtTokenSettings(secret, validMinutes, prolongMinutes);
		ssoHelper = new SsoHelper(tokenEndpoint, clientId, clientSecret, certEndpoint, null);
		SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(settings.getSecret()));
		flowParser = Jwts.parser().verifyWith(key).build();
	}

	@Override
	public User login(SSOLogin params, HttpServletRequest rq) {
		TokenResponse tokens;
		if (params.getUsername() == null)
			tokens = ssoHelper.codeExchange(params.getCode(), params.getRedirectUrl(), params.getNonce(), false, true);
		else {
			String username = params.getUsername();
			if (!username.contains("@") && !username.contains("\\"))
				username = username + "@" + domain;
			tokens = ssoHelper.passwordLogin(username, params.getPassword());
		}
		
		Claims userClaims = infoInIdToken ? tokens.getIdClaims() : tokens.getAccessClaims();
		SSOUser user = new SSOUser(userClaims.get(userIdProperty, String.class));
		user.setDisplayName(userClaims.get(userNameProperty, String.class));
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
			mapper.addMixIn(SSOUser.class, org.cibseven.webapp.auth.BaseUserProvider.UserSerialization.class);
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

	//ADFS doesn't support querying for users
	@Override
	public Collection<CIBUser> getUsers(User user, Optional<String> filter) {
	   try {
			Hashtable<String, String> environment = new Hashtable<String, String>();
	        environment.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	        environment.put(javax.naming.Context.PROVIDER_URL, serverURL);
	        environment.put(javax.naming.Context.SECURITY_PRINCIPAL, ldapUser);
	        environment.put(javax.naming.Context.SECURITY_CREDENTIALS, ldapPassword);
	        environment.put(javax.naming.Context.REFERRAL, ldapFollowReferrals);
			InitialDirContext initialDirContext = new InitialDirContext(environment);
			SearchControls searchControls = new SearchControls();
			searchControls.setCountLimit(ldapCountLimit);
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			searchControls.setReturningAttributes(new String[] { ldapNameAttribute, ldapDisplayNameAttribute });
			
			String initFilter = "(&(|";
			String filters = "";
			for (String f : ldapAttributesFilters) {
				filters += "(" + f + "=" + filter.orElse("") + "*)";
			}
			initFilter += filters + ")(objectClass=person))";
			log.debug(initFilter + " " + ldapFolder);
			
			NamingEnumeration<SearchResult> results = initialDirContext.search(ldapFolder, initFilter, searchControls);
			Collection<CIBUser> users = new ArrayList<>();
			try {
				if(!results.hasMore()) {
					throw new SystemException("[ERROR][AdfsUserProvider]Users not found in LDAP with the following filter: " + filter.orElse("") + "*");
				}
				
				while (results.hasMore()) {
				    SearchResult result = results.next();
				    CIBUser foundUser = new CIBUser(result.getAttributes().get(ldapNameAttribute).get().toString());
				    foundUser.setDisplayName(result.getAttributes().get(ldapDisplayNameAttribute).get().toString());
				    users.add(foundUser);
				}
			} catch (SizeLimitExceededException e) {
				log.debug("[INFO][AdfsUserProvider] Size limit exceeded");
			} catch (PartialResultException e) {
				log.debug("[INFO][AdfsUserProvider] Unprocessed continuation reference");
				log.info(e.getMessage());
			}
	        return users;
		} catch (NamingException x) {
			log.error("Failed to search for users", x);
			throw new SystemException(x);
		}
	}

	@Override
	public Object authenticateUser(HttpServletRequest request) {
		return authenticate(request);
	}
	
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
			Claims claims = ssoHelper.getKeyResolver().checkToken(token);
			SSOUser user = new SSOUser(technicalUserId);
			user.setDisplayName(claims.get("appid", String.class));
			user.setAuthToken(createToken(settings, false, false, user));
			return user;
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