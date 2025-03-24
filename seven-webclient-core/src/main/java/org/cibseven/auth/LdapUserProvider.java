package org.cibseven.auth;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.cibseven.exception.SystemException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.cib.auth.AuthenticationException;
import de.cib.auth.JwtUserProvider;
import de.cib.auth.LoginException;
import de.cib.auth.TokenExpiredException;
import de.cib.auth.User;
import de.cib.auth.rest.StandardLogin;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LdapUserProvider extends BaseUserProvider<StandardLogin> implements InitializingBean {
	
	@Value("${ldap.url:}") String serverURL;
	@Value("${ldap.user:}") String ldapUser;
	@Value("${ldap.password:}") String ldapPassword;
	@Value("${ldap.folder:}") String ldapFolder;
	@Value("${ldap.userNameAttribute:}") String ldapNameAttribute;
	@Value("${ldap.userDisplayNameAttribute:}") String ldapDisplayNameAttribute;
	@Value("#{'${ldap.attributes.filters:samAccountName;name}'.split(';\\s*')}") List<String> ldapAttributesFilters;
	@Value("${ldap.modifiedDateFormat:}") String ldapModifiedDateFormat;
	@Value("${ldap.countLimit:400}") int ldapCountLimit;
	@Value("${ldap.followReferrals:}") String ldapFollowReferrals;
	
	@Value("${authentication.jwtSecret:sekret}") String secret;	
	@Value("${authentication.tokenValidMinutes:60}") long validMinutes;	
	@Value("${authentication.tokenProlongMinutes:1440}") long prolongMinutes;
		
	public void afterPropertiesSet() {
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
			
			String initFilter = "(&(|";
			String filters = "";
			for (String f : ldapAttributesFilters) {
				filters += "(" + f + "=" + filter.orElse("") + "*)";
			}
			initFilter += filters + ")(objectClass=person))";
			
			NamingEnumeration<SearchResult> results = initialDirContext.search(ldapFolder, initFilter, searchControls);
			if(!results.hasMore()) {
				throw new SystemException("[ERROR][LdapUserProvider]Users not found in LDAP with the following filter: " + filter.orElse("") + "*");
			}
			
			Collection<CIBUser> users = new ArrayList<>();
			while (results.hasMore()) {
			    SearchResult result = results.next();
			    CIBUser foundUser = new CIBUser(result.getAttributes().get(ldapNameAttribute).get().toString());
			    foundUser.setDisplayName(result.getAttributes().get(ldapDisplayNameAttribute).get().toString());
			    users.add(foundUser);
			}
	        return users;
		} catch (NameNotFoundException | javax.naming.AuthenticationException x	) {
			throw new SystemException(x);
		} catch (NamingException e) {
			throw new SystemException(e);
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
