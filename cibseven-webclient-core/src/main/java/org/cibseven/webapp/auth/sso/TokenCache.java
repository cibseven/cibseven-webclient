package org.cibseven.webapp.auth.sso;

import java.util.Date;

public class TokenCache {
	private final String accessToken;
	private final Date expiration;
	private final Object lock = new Object(); 

	public TokenCache(String accessToken, Date expiration) {
		this.accessToken = accessToken;
		this.expiration = expiration;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public Date getExpiration() {
		return expiration;
	}

	public Object getLock() {
		return lock;
	}
}
