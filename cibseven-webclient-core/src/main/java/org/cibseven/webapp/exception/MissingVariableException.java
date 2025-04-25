package org.cibseven.webapp.exception;

public class MissingVariableException extends RuntimeException {

	private static final long serialVersionUID = -6903203826129435319L;

	public MissingVariableException(Throwable cause) {
		super("The variable could not be found!", cause);
	}
}
