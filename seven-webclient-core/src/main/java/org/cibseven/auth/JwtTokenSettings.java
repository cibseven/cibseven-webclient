package org.cibseven.auth;

import java.time.Duration;

import de.cib.auth.JwtUserProvider.TokenSettings;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Setter @AllArgsConstructor
public class JwtTokenSettings implements TokenSettings {

	String jwtSecret;	
	long tokenValidMinutes;	
	long tokenProlongMinutes;
	
	@Override
	public String getSecret() {
		return jwtSecret;
	}
	
	@Override
	public Duration getValid() {
		return Duration.ofMinutes(tokenValidMinutes);
	}
	
	@Override
	public Duration getProlong() {
		return Duration.ofMinutes(tokenProlongMinutes);
	}

}
