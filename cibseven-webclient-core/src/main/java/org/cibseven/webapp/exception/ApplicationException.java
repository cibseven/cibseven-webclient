package org.cibseven.webapp.exception;

import lombok.Getter;

abstract public class ApplicationException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	@Getter protected Object[] data;
	
	public ApplicationException(Object ...data) {
		this.data = data;
	}	
	
}
