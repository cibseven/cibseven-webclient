package org.cibseven.auth.sso;

import de.cib.auth.rest.StandardLogin;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class SSOLogin extends StandardLogin {
	String code;
	String nonce;
	String redirectUrl;
}
