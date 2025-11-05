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

import org.cibseven.webapp.auth.providers.JwtUserProvider;
import org.cibseven.webapp.auth.rest.StandardLogin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.jsonwebtoken.security.WeakKeyException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;

public abstract class BaseUserProvider<R extends StandardLogin> implements JwtUserProvider<R> {

	public abstract User login(R params, HttpServletRequest rq);
	public abstract void logout(User user);
	public abstract User getSelfInfoJSessionId(String userId, String jSessionId, HttpServletRequest rq);
	
	/**
	 * Authenticates user from request and validates engine consistency.
	 * This default implementation validates that the engine in the user object
	 * matches the X-Process-Engine header to prevent token reuse across engines.
	 */
	public Object authenticateUser(HttpServletRequest request) {
		Object result = authenticate(request);
		
		// Validate engine in user matches request header
		if (result instanceof CIBUser) {
			CIBUser user = (CIBUser) result;
			String requestEngine = request.getHeader("X-Process-Engine");
			
			// Validate engine in user object matches engine in request header
			if (user.getEngine() != null && requestEngine != null && !user.getEngine().equals(requestEngine)) {
				throw new org.cibseven.webapp.auth.exception.AuthenticationException("Token engine mismatch: user has '" + user.getEngine() + "' but request has '" + requestEngine + "'");
			}
		}
		
		return result;
	}
	
	@Getter
	protected JwtTokenSettings settings;

	public void checkKey() {
		var settings = getSettings();
		try {
			createKey(settings.getSecret());
		} catch(WeakKeyException | IllegalArgumentException e) {
			throw new IllegalArgumentException("Secret must be at least 155 characters long and a base64 decodable string");
		}
	}
	
	/**
	 * Sets the engine from the X-Process-Engine header to the user object.
	 * This should be called in login methods to store the engine with the user.
	 * @param user The user object (must be CIBUser or subclass)
	 * @param request The HTTP request containing the X-Process-Engine header
	 */
	protected void setEngineFromRequest(User user, HttpServletRequest request) {
		if (user instanceof CIBUser) {
			String engine = request.getHeader("X-Process-Engine");
			((CIBUser) user).setEngine(engine);
		}
	}
	
	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
	public static abstract class UserSerialization {
		@JsonIgnore abstract String getId();	
		@JsonIgnore abstract String getAuthToken();		
		@JsonIgnore abstract String getUrlToken();
		@JsonIgnore abstract String getEmail();
		@JsonIgnore abstract String getCallingSystem();
	}
	
	public abstract R createLoginParams();
	
	public String createExternalToken (StandardLogin params) {
		CIBUser user = new CIBUser(params.getUsername());
		return createToken(getSettings(), true, false, user);
	}
	
	public String getEngineRestToken(CIBUser user) {
		return user.getAuthToken();
	}

}
