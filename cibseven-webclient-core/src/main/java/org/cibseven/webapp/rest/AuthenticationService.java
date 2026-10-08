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

import java.util.Map;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.LogoutRedirectProvider;
import org.cibseven.webapp.auth.User;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.rest.model.Authorizations;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

@ApiResponses({ @ApiResponse(responseCode  = "500", description = "An unexpected system error occured") })
@RestController @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}" + "/auth")
public class AuthenticationService extends BaseService {
	
	@Operation(summary  = "Authenticates a user and returns (among other things) a token to be passed with other services")
	@ApiResponses({@ApiResponse(responseCode = "401", description = "Credentials are wrong") })
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public User login(@RequestBody Map<String, Object> data, HttpServletRequest rq) {
		String honeypot = (String) data.get("lastname");
		if (honeypot != null) return null;
	    //String username = (String) data.get("username");
	    //String password = (String) data.get("password");
		data.remove("lastname");
		StandardLogin standardLogin = new ObjectMapper().convertValue(data, StandardLogin.class);

	    return baseUserProvider.login(standardLogin, rq);
	}	
	
	@Operation(summary = "Ends the session of the user; a provider with a session at an external system also ends it there")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Contains 'logoutRedirectUrl' if the browser must also be sent to an external system to finish the logout") })
	@RequestMapping(value = "/logout", method = RequestMethod.POST)
	public Map<String, String> logout(User user, @RequestBody(required = false) Map<String, String> data) {
		String returnUrl = data == null ? null : data.get("returnUrl");
		baseUserProvider.logout(user);
		// Asked for after the logout, which is where the provider may pick up what the URL needs
		String redirectUrl = baseUserProvider instanceof LogoutRedirectProvider provider
			? provider.getLogoutRedirectUrl(user, returnUrl) : null;
		return redirectUrl == null ? Map.of() : Map.of("logoutRedirectUrl", redirectUrl);
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public User getSelfInfo(@NotNull User user, HttpServletRequest rq) {
		return baseUserProvider.getUserInfo(user, user.getId());
	}
	
	@RequestMapping(value = "/authorizations", method = RequestMethod.GET)
	public Authorizations getUserAuthorizations(@NotNull CIBUser user) {
		return bpmProvider.getUserAuthorization(user);
	}

}

