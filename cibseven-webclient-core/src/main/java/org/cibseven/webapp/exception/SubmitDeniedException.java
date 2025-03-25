package org.cibseven.webapp.exception;

public class SubmitDeniedException extends RuntimeException {

	private static final long serialVersionUID = -2365549217918889097L;


	public SubmitDeniedException(Throwable cause) {
		super("The submit could not be executed!", cause);
	}
}