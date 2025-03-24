package org.cibseven.webapp.rest;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import de.cib.auth.User;
import de.cib.auth.rest.StandardLogin;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.Authorizations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

@ApiResponses({ @ApiResponse(responseCode  = "500", description = "An unexpected system error occured") })
@RestController @RequestMapping("${services.basePath:/services/v1}" + "/auth")
public class AuthenticationService {
	
	@Autowired BaseUserProvider provider;
	@Autowired BpmProvider bpmProvider;
	
	@Operation(summary  = "Authenticates a user and returns (among other things) a token to be passed with other services")
	@ApiResponses({@ApiResponse(responseCode = "401", description = "Credentials are wrong") })
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public User login(@RequestBody Map<String, Object> data, HttpServletRequest rq) {
		String honeypot = (String) data.get("lastname");
		if (honeypot != null) return null;
	    String username = (String) data.get("username");
	    String password = (String) data.get("password");
		data.remove("lastname");
		StandardLogin standardLogin = new ObjectMapper().convertValue(data, StandardLogin.class);

	    return provider.login(standardLogin, rq);
	}	
	
	@RequestMapping(value = "/logout", method = RequestMethod.POST)
	public void logout(User user) {
		provider.logout(user);
	}	
	
	@RequestMapping(method = RequestMethod.GET)
	public User getSelfInfo(@NotNull User user, HttpServletRequest rq) {
		return provider.getUserInfo(user, user.getId());
	}

	@RequestMapping(value = "/users", method = RequestMethod.GET)
	public Collection<CIBUser> getUsers(@NotNull User user, @RequestParam Optional<String> filter) {
		return provider.getUsers(user, filter);
	}
	
	@RequestMapping(value = "/authorizations", method = RequestMethod.GET)
	public Authorizations getUserAuthorizations(@NotNull CIBUser user) {
		return bpmProvider.getUserAuthorization(user.getId(), user);
	}

	@RequestMapping(value = "/anon-user", method = RequestMethod.GET)
	public User getAnonymousUser() {
		return provider.createAnonToken();
	}

}

