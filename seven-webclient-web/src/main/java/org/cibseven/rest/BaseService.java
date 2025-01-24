package org.cibseven.rest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import de.cib.auth.AuthenticationException;
import de.cib.cibflow.CIBFlowUser;
import de.cib.cibflow.camunda.BpmProvider;
import de.cib.cibflow.api.exception.AnonUserBlockedException;
import de.cib.cibflow.api.rest.camunda.model.Authorizations;
import de.cib.cibflow.auth.FlowUserProvider;

public class BaseService {
	
	@Autowired BpmProvider bpmProvider;
	@Autowired FlowUserProvider flowUserProvider;
	
	protected CIBFlowUser checkAuthorization(HttpServletRequest rq, boolean basicAuthAllowed, boolean anonUserAllowed) {
		CIBFlowUser user = null;
		String authorization = rq.getHeader("Authorization");
		if (basicAuthAllowed && authorization != null && authorization.toLowerCase().startsWith("basic")) {
		    // Authorization: Basic base64credentials
		    String base64Credentials = authorization.substring("Basic".length()).trim();
		    byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
		    String credentials = new String(credDecoded, StandardCharsets.UTF_8);
		    // credentials = username:password
		    final String[] values = credentials.split(":", 2);
		    de.cib.auth.rest.StandardLogin login = flowUserProvider.createLoginParams();
		    login.setUsername(values[0]);
		    login.setPassword(values[1]);
		    user = (CIBFlowUser) flowUserProvider.login(login, rq);
		} 
		else {
			try {
				user = (CIBFlowUser) flowUserProvider.authenticateUser(rq);
			} catch (AnonUserBlockedException e) {
				if (anonUserAllowed) {
					user = (CIBFlowUser) e.getUser();
				} else {
					throw e;
				}
			}
		}
		return user;
	}
	
	public void checkSpecificProcessRights(CIBFlowUser user, String processKey) {
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		if (!flowUserProvider.hasSpecificProcessRights(authorizations, processKey))
			throw new AuthenticationException("You are not authorized to do this");
	}
	
	public void checkCockpitRights(CIBFlowUser user) {
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		if (!flowUserProvider.hasCockpitRights(authorizations)) {
			throw new AuthenticationException("You are not authorized to do this");
		}
	}
	
}
