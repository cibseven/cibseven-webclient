package org.cibseven.webapp.rest;

import lombok.Data;

@Data
public class PasswordRecoveryData {
	private String id;
	private String email;
}
