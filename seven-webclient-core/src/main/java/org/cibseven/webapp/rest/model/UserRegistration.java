package org.cibseven.webapp.rest.model;

import org.cibseven.webapp.auth.rest.StandardLogin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @NoArgsConstructor @ToString
public class UserRegistration extends StandardLogin {	

	String email;
	
	public UserRegistration(String username, String password, String email) {
		super(username, password);
		this.email = email;	
	}	
	
}
