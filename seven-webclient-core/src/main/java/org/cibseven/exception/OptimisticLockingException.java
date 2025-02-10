package org.cibseven.exception;

public class OptimisticLockingException extends ApplicationException {

	private static final long serialVersionUID = 1L;

	public OptimisticLockingException(Throwable cause) {
		super("Entity was updated by another transaction concurrently.", cause);
	}

}
