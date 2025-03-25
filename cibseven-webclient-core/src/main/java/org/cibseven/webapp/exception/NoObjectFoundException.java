package org.cibseven.webapp.exception;

public class NoObjectFoundException extends ApplicationException {

	private static final long serialVersionUID = -92972413203905944L;

	public NoObjectFoundException(Throwable cause) {
		super("The object could not be found!", cause);
	}

}