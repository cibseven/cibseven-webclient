package org.cibseven.exception;

public class ExistingGroupRequestException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public ExistingGroupRequestException(Throwable cause) {
		super("The group already exists", cause);
	}
	
}
