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

import java.util.Collection;
import java.util.Optional;

import org.cibseven.webapp.auth.providers.JwtUserProvider;
import org.cibseven.webapp.auth.rest.StandardLogin;

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
	
	public abstract R createLoginParams();
	
	public String createExternalToken (StandardLogin params) {
		CIBUser user = new CIBUser(params.getUsername());
		return createToken(getSettings(), true, false, user);
	}

}
