package org.cibseven.webapp.auth.sso;

import org.cibseven.webapp.auth.rest.StandardLogin;

import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class SSOLogin extends StandardLogin {
	String code;
	String nonce;
	String redirectUrl;
}
