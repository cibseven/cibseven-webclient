package org.cibseven.auth.sso;

import org.cibseven.auth.CIBUser;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class SSOUser extends CIBUser {
	
	@Getter @Setter
	String refreshToken;
	
	public SSOUser(String userId) {
		super(userId);
	}
}
