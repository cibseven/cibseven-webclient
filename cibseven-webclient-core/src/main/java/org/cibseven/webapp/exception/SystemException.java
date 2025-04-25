package org.cibseven.webapp.exception;

import lombok.NonNull;

public class SystemException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public SystemException(@NonNull Throwable cause) {
		super("Some unexpected technical problem occured", cause);
	}
	
	public SystemException(@NonNull String msg) {
		super("Some unexpected technical problem occured: " + msg);
	}

	public SystemException(@NonNull String msg, @NonNull Throwable cause) {
		super("Some unexpected technical problem occured: " + msg, cause);
	}
	
}
