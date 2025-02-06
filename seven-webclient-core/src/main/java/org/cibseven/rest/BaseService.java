package org.cibseven.rest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import de.cib.auth.AuthenticationException;
import de.cib.auth.rest.StandardLogin;

import org.cibseven.auth.CIBUser;
import org.cibseven.auth.BaseUserProvider;
import org.cibseven.exception.AnonUserBlockedException;
import org.cibseven.providers.BpmProvider;
import org.cibseven.rest.model.Authorizations;

public class BaseService {
	
	@Autowired
	protected BpmProvider bpmProvider;
	@Autowired
	protected BaseUserProvider baseUserProvider;
	
	protected CIBUser checkAuthorization(HttpServletRequest rq, boolean basicAuthAllowed, boolean anonUserAllowed) {
		CIBUser user = null;
		String authorization = rq.getHeader("Authorization");
		if (basicAuthAllowed && authorization != null && authorization.toLowerCase().startsWith("basic")) {
		    // Authorization: Basic base64credentials
		    String base64Credentials = authorization.substring("Basic".length()).trim();
		    byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
		    String credentials = new String(credDecoded, StandardCharsets.UTF_8);
		    // credentials = username:password
		    final String[] values = credentials.split(":", 2);
		    StandardLogin login = baseUserProvider.createLoginParams();
		    login.setUsername(values[0]);
		    login.setPassword(values[1]);
		    user = (CIBUser) baseUserProvider.login(login, rq);
		} 
		else {
			try {
				user = (CIBUser) baseUserProvider.authenticateUser(rq);
			} catch (AnonUserBlockedException e) {
				if (anonUserAllowed) {
					user = (CIBUser) e.getUser();
				} else {
					throw e;
				}
			}
		}
		return user;
	}
	
	public void checkSpecificProcessRights(CIBUser user, String processKey) {
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		if (!baseUserProvider.hasSpecificProcessRights(authorizations, processKey))
			throw new AuthenticationException("You are not authorized to do this");
	}
	
	public void checkCockpitRights(CIBUser user) {
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		if (!baseUserProvider.hasCockpitRights(authorizations)) {
			throw new AuthenticationException("You are not authorized to do this");
		}
	}
	
}
