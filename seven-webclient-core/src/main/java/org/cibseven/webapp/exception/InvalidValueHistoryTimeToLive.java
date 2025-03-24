package org.cibseven.webapp.exception;

public class InvalidValueHistoryTimeToLive extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public InvalidValueHistoryTimeToLive(Throwable cause) {
		super("Null historyTimeToLive values are not allowed", cause);
	}

}
