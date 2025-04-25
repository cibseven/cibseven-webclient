package org.cibseven.webapp.exception;

public class AccessDeniedException extends RuntimeException { //TODO ? remove
	
	private static final long serialVersionUID = 1L;

	public AccessDeniedException(String object) {
		super(object);
	}

}
