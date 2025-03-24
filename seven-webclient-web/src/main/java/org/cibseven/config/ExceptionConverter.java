package org.cibseven.config;

import java.util.Arrays;

import org.cibseven.exception.ApplicationException;
import org.cibseven.exception.ErrorMessage;
import org.cibseven.exception.NoObjectFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import de.cib.auth.AuthenticationException;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice @ResponseBody
@Slf4j
public class ExceptionConverter {
	
	@ExceptionHandler @ResponseStatus(HttpStatus.UNAUTHORIZED) 
	public ErrorMessage authentication(AuthenticationException x) {
		log.debug("Authentication error occured " + Arrays.toString(x.getData()), x);
		return new ErrorMessage(x.getClass().getSimpleName(), x.getData());
	}
	
	@ExceptionHandler @ResponseStatus(HttpStatus.BAD_REQUEST) 
	public ErrorMessage application(ApplicationException x) {
		log.info("Application error occured " + Arrays.toString(x.getData()), x);
		return new ErrorMessage(x.getClass().getSimpleName(), x.getData());
	}
	
	@ExceptionHandler @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ErrorMessage system(RuntimeException x) {
		log.error("System error occured", x);
		return new ErrorMessage(x.getClass().getSimpleName(), new Object[] { x.getMessage() });
	}
	
	@ExceptionHandler @ResponseStatus(HttpStatus.NOT_FOUND) 
	public ErrorMessage application(NoObjectFoundException x) {
		log.debug("Application error occured " + Arrays.toString(x.getData()), x);
		return new ErrorMessage("NoObjectFoundException");
	}
	
}
	