package org.cibseven.webapp.exception;

public class ExpressionEvaluationException extends RuntimeException {

	private static final long serialVersionUID = -6659684074308234448L;


	public ExpressionEvaluationException(Throwable cause) {
		super("The process cannot be started as an expression cannot be evaluated!", cause);
	}
}