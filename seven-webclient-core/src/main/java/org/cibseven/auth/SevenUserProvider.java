package org.cibseven.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;

import javax.crypto.SecretKey;

import jakarta.servlet.http.HttpServletRequest;

import org.cibseven.providers.BpmProvider;
import org.cibseven.providers.SevenProvider;
import org.cibseven.rest.model.SevenUser;
import org.cibseven.rest.model.SevenVerifyUser;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.cib.auth.AuthenticationException;
import de.cib.auth.JwtUserProvider;
import de.cib.auth.TokenExpiredException;
import de.cib.auth.User;
import de.cib.auth.rest.StandardLogin;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class SevenUserProvider extends BaseUserProvider<StandardLogin> implements InitializingBean {
	
	@Value("${authentication.jwtSecret:sekret}") String secret;	
	@Value("${authentication.tokenValidMinutes:60}") long validMinutes;	
	@Value("${authentication.tokenProlongMinutes:1440}") long prolongMinutes;
	
	@Value("${camunda.engineRest.url}") String camundaUrl;
	
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
				SevenUser cUser = sevenProvider.getUserProfile(user.getId(), user);
				user.setDisplayName(cUser.getFirstName() + " " + cUser.getLastName());
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
