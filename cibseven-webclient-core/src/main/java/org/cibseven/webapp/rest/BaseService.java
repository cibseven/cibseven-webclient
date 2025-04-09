package org.cibseven.webapp.rest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.rest.StandardLogin;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenAuthorizationUtils;
import org.cibseven.webapp.exception.AnonUserBlockedException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.Authorizations;

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
		if (!SevenAuthorizationUtils.hasSpecificProcessRights(authorizations, processKey))
			throw new AuthenticationException("You are not authorized to do this");
	}
	
	public void checkCockpitRights(CIBUser user) {
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		if (!SevenAuthorizationUtils.hasCockpitRights(authorizations)) {
			throw new AuthenticationException("You are not authorized to do this");
		}
	}
	
	public void checkPermission(CIBUser user, String type, List<String> permissions) {
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		if (!SevenAuthorizationUtils.checkPermission(authorizations, type, permissions)) {
			throw new AuthenticationException("You are not authorized to do this");
		}
	}	
	
	public void hasAdminManagementPermissions(CIBUser user, String action, String type, List<String> permissions) {
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		if (!SevenAuthorizationUtils.hasAdminManagementPermissions(authorizations, action, type, permissions)) {
			throw new AuthenticationException("You are not authorized to do this");
		}
	}	
	
}
