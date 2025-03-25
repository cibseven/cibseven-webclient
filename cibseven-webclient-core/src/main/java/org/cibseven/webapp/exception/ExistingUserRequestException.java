package org.cibseven.webapp.exception;

public class ExistingUserRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ExistingUserRequestException(Throwable cause) {
		super("The user already exists", cause);
	}
}
