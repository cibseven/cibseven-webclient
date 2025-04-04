package org.cibseven.webapp.auth;

import java.util.Collection;
import java.util.Optional;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.providers.JwtUserProvider;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.exception.AnonUserBlockedException;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public abstract class BaseUserProvider<R extends StandardLogin> implements JwtUserProvider<R> {

	public abstract User login(R params, HttpServletRequest rq);
	public abstract void logout(User user);
	public abstract User getSelfInfoJSessionId(String userId, String jSessionId, HttpServletRequest rq);
	public abstract Collection<CIBUser> getUsers(@NotNull User user, Optional<String> filter);
	public abstract Object authenticateUser(HttpServletRequest request);
	
	@Value("${user.anon.id:}")
	String anonUserId;

	@Getter
	protected JwtTokenSettings settings;
	
	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
	public static abstract class UserSerialization {
		@JsonIgnore abstract String getId();	
		@JsonIgnore abstract String getAuthToken();		
		@JsonIgnore abstract String getUrlToken();
		@JsonIgnore abstract String getEmail();
		@JsonIgnore abstract String getCallingSystem();
	}
	
	@Override
	public User authenticate(HttpServletRequest rq) {
		User user = JwtUserProvider.super.authenticate(rq);
		if (user instanceof CIBUser && ((CIBUser) user).isAnonUser()) {
			throw new AnonUserBlockedException(user);
		}
		return user;
	}

	public abstract R createLoginParams();
	
	public String createExternalToken (StandardLogin params) {
		CIBUser user = new CIBUser(params.getUsername());
		return createToken(getSettings(), true, false, user);
	}

	public User createAnonToken () {
		if (anonUserId == null || anonUserId.isEmpty()) {
			throw new AuthenticationException("Anon user is not configured");
		}
		CIBUser user = new CIBUser(anonUserId);
		user.setAnonUser(true);
		user.setAuthToken(createToken(getSettings(), true, false, user));
		return user;
	}
}
