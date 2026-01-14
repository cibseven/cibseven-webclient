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
import org.springframework.beans.factory.annotation.Value;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.auth.rest.StandardLogin;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenAuthorizationUtils;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.Authorizations;

public class BaseService {

	@Autowired
	protected BpmProvider bpmProvider;
	@SuppressWarnings("rawtypes")
	@Autowired
	protected BaseUserProvider baseUserProvider;
	
	@Value("${camunda.bpm.authorization.enabled:false}")
	private boolean authorizationEnabled;

	protected CIBUser checkAuthorization(HttpServletRequest rq, boolean basicAuthAllowed) {
		CIBUser user = (CIBUser) baseUserProvider.checkAuthorization(rq, basicAuthAllowed);
		return user;
	}

	public void checkSpecificProcessRights(CIBUser user, String processKey) {
		if (!authorizationEnabled) return;	
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		// hasSpecificProcessRights now throws detailed AccessDeniedException when permission check fails
		SevenAuthorizationUtils.hasSpecificProcessRights(authorizations, processKey);
	}

	public void checkCockpitRights(CIBUser user) {
		if (!authorizationEnabled) return;	
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		// hasCockpitRights now throws detailed AccessDeniedException when permission check fails
		SevenAuthorizationUtils.hasCockpitRights(authorizations);
	}

	public void checkPermission(CIBUser user, SevenResourceType type, List<String> permissions) {
		if (!authorizationEnabled) return;	
		Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
		// SevenAuthorizationUtils.checkPermission now throws detailed AccessDeniedException when permission check fails
		SevenAuthorizationUtils.checkPermission(authorizations, type, permissions);
	}
}
