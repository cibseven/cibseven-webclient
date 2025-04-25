package org.cibseven.webapp.exception;

import java.util.Optional;

public class InvalidAttributeValueException extends RuntimeException {

	private static final long serialVersionUID = -3122514142168860236L;


	public InvalidAttributeValueException(Optional<String> invalidValue, Throwable cause) {
		super(String.format("The value \"%s\" is not valid!", invalidValue.orElseGet(() -> "(empty)")), cause);
	}
}