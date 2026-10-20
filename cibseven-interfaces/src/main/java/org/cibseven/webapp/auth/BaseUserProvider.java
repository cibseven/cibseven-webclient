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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.cibseven.webapp.auth.providers.JwtUserProvider;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.config.EngineRestProperties;
import org.cibseven.webapp.config.SevenWebclientProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.jsonwebtoken.security.WeakKeyException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;

public abstract class BaseUserProvider<R extends StandardLogin> implements JwtUserProvider<R> {

	// Field instead of constructor injection: a constructor parameter would have to be repeated by every
	// subclass, also outside this repository. Defaults apply until Spring injects the bound instance.
	@Autowired(required = false)
	protected SevenWebclientProperties properties = new SevenWebclientProperties();
	
	protected long getValidMinutes() {
		return properties.getAuthentication().getTokenValidMinutes();
	}

	protected long getProlongMinutes() {
		return properties.getAuthentication().getTokenProlongMinutes();
	}

	protected EngineRestProperties getEngineRestProperties() {
		return properties.getEngineRest();
	}

	/**
	 * @param property name relative to {@code cibseven.webclient}
	 * @throws IllegalStateException if a property required by this provider is not configured
	 */
	protected static String required(String value, String property) {
		if (value == null) {
			throw new IllegalStateException(SevenWebclientProperties.PREFIX + "." + property + " must be configured for " + "this user provider");
		}
		return value;
	}

	public abstract User login(R params, HttpServletRequest rq);
	public abstract void logout(User user);
	public abstract User getSelfInfoJSessionId(String userId, String jSessionId, HttpServletRequest rq);

	/**
	 * Checks authorization from request. If basicAuthAllowed is true, it will also check for Basic Auth header.
	 * @return The authenticated CIBUser
	 */
	public CIBUser checkAuthorization(HttpServletRequest rq, boolean basicAuthAllowed) {
		CIBUser user = null;
		String authorization = rq.getHeader("Authorization");
		if (basicAuthAllowed && authorization != null && authorization.toLowerCase().startsWith("basic")) {
			// Authorization: Basic base64credentials
			String base64Credentials = authorization.substring("Basic".length()).trim();
			byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
			String credentials = new String(credDecoded, StandardCharsets.UTF_8);
			// credentials = username:password
			final String[] values = credentials.split(":", 2);
			StandardLogin login = createLoginParams();
			login.setUsername(values[0]);
			login.setPassword(values[1]);
			user = (CIBUser) login((R)login, rq);
		} 
		else {
			user = (CIBUser) authenticateUser(rq);
		}
		return user;
	}

	/**
	 * Authenticates user from request.
	 */
	public Object authenticateUser(HttpServletRequest request) {
		return authenticate(request);
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
