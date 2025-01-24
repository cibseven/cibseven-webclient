package org.cibseven.rest;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.cib.auth.User;
import de.cib.cibflow.api.PasswordRecoverProvider;
import de.cib.cibflow.api.exception.AccessDeniedException;
import de.cib.cibflow.api.rest.camunda.model.Authorizations;
import de.cib.cibflow.api.rest.camunda.model.CamundaUser;
import de.cib.cibflow.auth.CamundaUserProvider;
import de.cib.cibflow.auth.FlowUserProvider;
import de.cib.cibflow.CIBFlowUser;
import de.cib.cibflow.camunda.BpmProvider;
import de.cib.cibflow.rest.StandardLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({ @ApiResponse(responseCode  = "500", description = "An unexpected system error occured") })
@RestController @RequestMapping("/auth")
public class AuthenticationService {
	
	@Autowired FlowUserProvider provider;
	@Autowired BpmProvider bpmProvider;
	@Autowired(required = false) PasswordRecoverProvider passwordRecoverProvider;
	
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
	public Collection<CIBFlowUser> getUsers(@NotNull User user, @RequestParam Optional<String> filter) {
		return provider.getUsers(user, filter);
	}
	
	@RequestMapping(value = "/authorizations", method = RequestMethod.GET)
	public Authorizations getUserAuthorizations(@NotNull CIBFlowUser user) {
		return bpmProvider.getUserAuthorization(user.getId(), user);
	}

	@RequestMapping(value = "/anon-user", method = RequestMethod.GET)
	public User getAnonymousUser() {
		return provider.createAnonToken();
	}
	
	
	@RequestMapping(value = "/password-recover", method = RequestMethod.POST)
	public void passwordRecover(@RequestBody PasswordRecoveryData data, HttpServletRequest rq, Locale locale) {
		if (provider instanceof CamundaUserProvider) {
			CamundaUser camundaUser = bpmProvider.getUserProfile(data.getId(), null);
			if (camundaUser.getEmail() != null && !camundaUser.getEmail().isEmpty()) {
				passwordRecoverProvider.passwordRecover(data.getId(), camundaUser.getEmail(), locale);
			} else {
				throw new AccessDeniedException("User does not have an email assigned");
			}
		}
	}
	
	@RequestMapping(value = "/password-recover-check", method = RequestMethod.GET)
	public Boolean passwordRecoverCheck(HttpServletRequest rq) {
		if (provider instanceof CamundaUserProvider) {
			return passwordRecoverProvider.passwordRecoverCheck(rq.getHeader("Authorization"));
		} else {
			return false;
		}
	}

	@RequestMapping(value = "/password-recovery-update-password/{userId}", method = RequestMethod.PUT)
	public Boolean updateUserPassword(@PathVariable String userId, @RequestBody Map<String, Object> data, Locale loc, HttpServletRequest rq) {
		if (provider instanceof CamundaUserProvider) {
			passwordRecoverProvider.passwordRecoverCheck(rq.getHeader("Authorization"));
			bpmProvider.updateUserCredentials(userId, data, null);
			return true;
		} else {
			return false;
		}
	}	
}

