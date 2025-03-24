package org.cibseven.auth.sso;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jsonwebtoken.Claims;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class TokenResponse {
	String access_token;
	long expires_in;
	long refresh_expires_in;
	String refresh_token;
	String id_token;
	String token_type;
	String scope;
	
	//Storage for resolved tokens
	@JsonIgnore
	Claims idClaims;
	@JsonIgnore
	Claims accessClaims;
}