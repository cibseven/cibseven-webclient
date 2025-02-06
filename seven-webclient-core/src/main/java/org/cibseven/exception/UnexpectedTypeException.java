package org.cibseven.exception;

public class UnexpectedTypeException extends RuntimeException {

	private static final long serialVersionUID = -7007910135736694086L;


	public UnexpectedTypeException(Throwable cause) {
		super("The value type of a process variable does not match the expected type!", cause);
	}
}