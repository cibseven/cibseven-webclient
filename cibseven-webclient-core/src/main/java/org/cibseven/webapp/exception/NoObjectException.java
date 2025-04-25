package org.cibseven.webapp.exception;

public class NoObjectException extends ApplicationException { //TODO ? remove
	
	private static final long serialVersionUID = 1L;

	public NoObjectException(String id) {
		super(id);
	}

}
