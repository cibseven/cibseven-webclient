package org.cibseven.webapp.rest;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @ToString @NoArgsConstructor
public class StandardLogin extends de.cib.auth.rest.StandardLogin { //TODO ? use for automatic mask generation
	
	public StandardLogin(String username, String password) {
		super(username, password);
	}
}
