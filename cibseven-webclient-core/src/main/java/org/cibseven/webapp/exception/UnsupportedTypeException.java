package org.cibseven.webapp.exception;

public class UnsupportedTypeException extends RuntimeException {

	private static final long serialVersionUID = -7007910135736694086L;


	public UnsupportedTypeException(Throwable cause) {
		super("The process cannot be started because of an unsupported value type!", cause);
	}
}