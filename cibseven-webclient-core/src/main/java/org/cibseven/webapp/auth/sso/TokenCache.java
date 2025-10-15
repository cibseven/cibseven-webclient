package org.cibseven.webapp.auth.sso;

import java.util.Date;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TokenCache {
	private final String accessToken;
	private final Date expiration;
}
