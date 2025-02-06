package org.cibseven.exception;

public class PasswordPolicyException extends RuntimeException {

	private static final long serialVersionUID = -291916120097487810L;

	public PasswordPolicyException(Throwable cause) {
		super("Password does not match policy", cause);
	}
}
