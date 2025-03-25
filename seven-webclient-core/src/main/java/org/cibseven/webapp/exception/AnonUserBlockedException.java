package org.cibseven.webapp.exception;

import org.cibseven.webapp.auth.User;
import org.cibseven.webapp.auth.exception.AuthenticationException;

import lombok.Getter;

public class AnonUserBlockedException extends AuthenticationException {
	
	private static final long serialVersionUID = 1L;
	
	@Getter
	private User user;

	public AnonUserBlockedException(User user) {
		super();
		this.user = user;
	}

}
