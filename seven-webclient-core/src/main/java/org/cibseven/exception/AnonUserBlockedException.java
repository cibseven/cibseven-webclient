package org.cibseven.exception;

import de.cib.auth.AuthenticationException;
import de.cib.auth.User;
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
