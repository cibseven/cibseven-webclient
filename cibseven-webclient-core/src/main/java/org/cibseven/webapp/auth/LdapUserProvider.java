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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.crypto.SecretKey;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.LoginException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.auth.providers.JwtUserProvider;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.exception.SystemException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LdapUserProvider extends BaseUserProvider<StandardLogin> {
	
	@Value("${cibseven.webclient.ldap.url:}") String serverURL;
	@Value("${cibseven.webclient.ldap.user:}") String ldapUser;
	@Value("${cibseven.webclient.ldap.password:}") String ldapPassword;
	@Value("${cibseven.webclient.ldap.folder:}") String ldapFolder;
	@Value("${cibseven.webclient.ldap.userNameAttribute:}") String ldapNameAttribute;
	@Value("${cibseven.webclient.ldap.userDisplayNameAttribute:}") String ldapDisplayNameAttribute;
	@Value("#{'${cibseven.webclient.ldap.attributes.filters:samAccountName;name}'.split(';\\s*')}") List<String> ldapAttributesFilters;
	@Value("${cibseven.webclient.ldap.modifiedDateFormat:}") String ldapModifiedDateFormat;
	@Value("${cibseven.webclient.ldap.countLimit:400}") int ldapCountLimit;
	@Value("${cibseven.webclient.ldap.followReferrals:}") String ldapFollowReferrals;
	
	@Value("${cibseven.webclient.authentication.jwtSecret:}") String secret;
	@Value("${cibseven.webclient.authentication.tokenValidMinutes:60}") long validMinutes;
	@Value("${cibseven.webclient.authentication.tokenProlongMinutes:1440}") long prolongMinutes;
	
	@PostConstruct
	public void init() {
		settings = new JwtTokenSettings(secret, validMinutes, prolongMinutes);		
	}

	@Override
	public CIBUser login(StandardLogin login, HttpServletRequest rq) {	
        try {
			Hashtable<String, String> environment = new Hashtable<String, String>();
	        environment.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	        environment.put(javax.naming.Context.PROVIDER_URL, serverURL);
	        environment.put(javax.naming.Context.SECURITY_PRINCIPAL, getFullUserDN(login.getUsername()));
	        environment.put(javax.naming.Context.SECURITY_CREDENTIALS, login.getPassword());
	        environment.put(javax.naming.Context.REFERRAL, ldapFollowReferrals);
			InitialDirContext initialDirContext = new InitialDirContext(environment);
			SearchControls searchControls = new SearchControls();
			searchControls.setCountLimit(ldapCountLimit);
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration<SearchResult> results = initialDirContext.search(ldapFolder, "(&(" + ldapNameAttribute + "=" + login.getUsername() + ")(objectClass=person))", searchControls);
			if(!results.hasMore()) {
				throw new LoginException("[ERROR][LdapUserProvider] login not user found with the following username: " + login.getUsername());
			}
			SearchResult result = results.next();
			CIBUser user =  new CIBUser(result.getAttributes().get(ldapNameAttribute).get().toString());
			user.setDisplayName(result.getAttributes().get(ldapDisplayNameAttribute).get().toString());
			user.setAuthToken(createToken(getSettings(), true, false, user));
	        
	        return user;
		} catch (NamingException x) {
			throw new LoginException();
		}
	}
	
	@Override
	public void logout(User user) {
		
	}

	private String getFullUserDN(String userName) {
		try {
			log.debug("Searching full DN name of user " + userName);
			Hashtable<String, String> environment = new Hashtable<String, String>();
	        environment.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	        environment.put(javax.naming.Context.PROVIDER_URL, serverURL);
	        environment.put(javax.naming.Context.SECURITY_PRINCIPAL, ldapUser);
	        environment.put(javax.naming.Context.SECURITY_CREDENTIALS, ldapPassword);
			InitialDirContext initialDirContext;
			initialDirContext = new InitialDirContext(environment);
			SearchControls searchControls = new SearchControls();
			searchControls.setCountLimit(ldapCountLimit);
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration<SearchResult> results = initialDirContext.search(ldapFolder, "(&(" + ldapNameAttribute + "=" + userName + "))", searchControls);
			if(!results.hasMore()) {
				throw new LoginException("[ERROR][LdapUserProvider] getFullUserDN not user found with the following username: " + userName);
			}
			SearchResult result = results.next();
			log.debug("Ldap result: user full DN " + result.getNameInNamespace());
			return result.getNameInNamespace();
		} catch (NamingException x) {
			throw new LoginException();
		}
	}

	@Override
	public User getUserInfo(User user, String userId) {
		if (user.getId().compareTo(userId) == 0) {
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
			mapper.addMixIn(CIBUser.class, UserSerialization.class);
			return mapper.writeValueAsString(user);
		} catch (JsonProcessingException x) {
			throw new SystemException(x);
		}
	}

	@Override
	public User verify(Claims claim) {
		return null;
	}
	
	public User verify(Claims userClaims, Date issuedAt) {
		Hashtable<String, String> environment = new Hashtable<String, String>();
        environment.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(javax.naming.Context.PROVIDER_URL, serverURL);
        environment.put(javax.naming.Context.SECURITY_PRINCIPAL, ldapUser);
        environment.put(javax.naming.Context.SECURITY_CREDENTIALS, ldapPassword);
        environment.put(javax.naming.Context.REFERRAL, ldapFollowReferrals);
        try {
			InitialDirContext initialDirContext = new InitialDirContext(environment);
			SearchControls searchControls = new SearchControls();
			searchControls.setReturningAttributes(new String[] {"modifyTimestamp", ldapDisplayNameAttribute});
			searchControls.setCountLimit(ldapCountLimit);
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration<SearchResult> results = initialDirContext.search(ldapFolder, "(&(" + ldapNameAttribute + "=" + userClaims.getSubject() + "))", searchControls);
			if(!results.hasMore()) {
				return null;
			}
			SearchResult result = results.next();
			String changed = result.getAttributes().get("modifyTimestamp").get().toString();
			SimpleDateFormat dateFormatter = new SimpleDateFormat(ldapModifiedDateFormat);
			try {
				if(dateFormatter.parse(changed).after(issuedAt)) {
					return null;
				}
			} catch (ParseException e) {
				return null;
			}
			CIBUser user =  (CIBUser) deserialize(userClaims.get("user").toString(), null);
			user.setDisplayName(result.getAttributes().get(ldapDisplayNameAttribute).get().toString());
	        user.setAuthToken(createToken(getSettings(), true, false, user));
	        
	        return user;
        } catch(NamingException e) {
        	throw new SystemException(e);
        }
	}

	public User parse(String token, TokenSettings settings) {
		try {			
			SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(settings.getSecret()));
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			User user = deserialize((String) claims.get("user"), JwtUserProvider.BEARER_PREFIX + token);
			if ((boolean) claims.get("verify") && verify(claims, claims.getIssuedAt()) == null)
				throw new AuthenticationException(token);

			return user;
			
		} catch (ExpiredJwtException x) {
			long ageMillis = System.currentTimeMillis() - x.getClaims().getExpiration().getTime();
			if ((boolean) x.getClaims().get("prolongable") && (ageMillis < settings.getProlong().toMillis())) {
				User user = verify(x.getClaims(), x.getClaims().getIssuedAt());
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
