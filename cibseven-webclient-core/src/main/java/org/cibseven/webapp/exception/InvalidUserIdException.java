package org.cibseven.webapp.exception;

public class InvalidUserIdException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public InvalidUserIdException(Throwable cause) {
		super("User has an invalid id", cause);
	}

}
