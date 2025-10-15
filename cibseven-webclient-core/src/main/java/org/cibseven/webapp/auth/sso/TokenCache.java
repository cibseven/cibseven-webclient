package org.cibseven.webapp.auth.sso;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TokenCache {
	private String accessToken;
	private Date expiration;
	private boolean currentlyFetched;
}
