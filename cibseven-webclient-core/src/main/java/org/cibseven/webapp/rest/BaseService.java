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
package org.cibseven.webapp.rest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.SevenResourceType;
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
	
	public void checkPermission(CIBUser user, SevenResourceType type, List<String> permissions) {
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		if (!SevenAuthorizationUtils.checkPermission(authorizations, type, permissions)) {
			throw new AuthenticationException("You are not authorized to do this");
		}
	}	
}
