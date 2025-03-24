package org.cibseven.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor @NoArgsConstructor @Getter 
public class ErrorMessage {
	
	String type;
	Object[] params;
	
	public ErrorMessage(String type) {
		this.type = type;
	}
}