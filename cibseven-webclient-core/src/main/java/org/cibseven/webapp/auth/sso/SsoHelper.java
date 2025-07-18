/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.cibseven.webapp.auth.sso;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.exception.SystemException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SsoHelper {
	
	String tokenEndpoint, clientId, clientSecret, userInfoEndpoint, introspectionEndpoint;

	HttpHeaders formUrlEncodedHeader = new HttpHeaders();
	{
		formUrlEncodedHeader.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
	}
	
	@Getter
	KeyResolver keyResolver;
	
	public SsoHelper(String tokenEndpoint, String clientId, String clientSecret, String certEndpoint, String userInfoEndpoint, String introspectionEndpoint) {
		this.tokenEndpoint = tokenEndpoint;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.userInfoEndpoint = userInfoEndpoint;
		this.introspectionEndpoint = introspectionEndpoint;
		keyResolver = new KeyResolver(certEndpoint);
	}
	
	public TokenResponse codeExchange(String code, String redirectUrl, String nonce) {
		return codeExchange(code, redirectUrl, nonce, true, true);
	}
	
	public TokenResponse codeExchange(String code, String redirectUrl, String nonce, boolean nonceInAccess, boolean nonceInId) {
		MultiValueMap<String, String> rqParams = new LinkedMultiValueMap<>();
		rqParams.add("client_id", clientId);
		rqParams.add("client_secret", clientSecret);
		rqParams.add("code", code);
		rqParams.add("grant_type", "authorization_code");
		rqParams.add("redirect_uri", redirectUrl); //https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.3.2

		TokenResponse tokens = null;
		try {
			HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(rqParams, formUrlEncodedHeader);
			RestTemplate template = new RestTemplate();
			tokens = template.postForObject(tokenEndpoint, tokenRequest, TokenResponse.class);
			if (tokens != null) log.debug(tokens.getId_token());
			if (tokens != null) log.debug(tokens.getAccess_token());
		} catch (RestClientResponseException e) {
			throw new AuthenticationException(e.getResponseBodyAsString());
		}

		if (tokens != null) {
			String hashedNonce = hashString(nonce);
			Claims accessClaims = null;
			
			if(keyResolver.isJwt(tokens.getAccess_token())) { //access token can just be a random String
				accessClaims = keyResolver.checkToken(tokens.getAccess_token());				
			}
			
			Claims idClaims = keyResolver.checkToken(tokens.getId_token());
			if ((nonceInAccess && accessClaims != null && !hashedNonce.equals(accessClaims.get("nonce")))
		            || (nonceInId && !hashedNonce.equals(idClaims.get("nonce"))))
				throw new AuthenticationException("Nonce check failed; hashed nonce: " + hashedNonce);
			
			tokens.accessClaims = accessClaims;
			tokens.idClaims = idClaims;
		}
		return tokens;
	}
	
	public TokenResponse refreshToken(String refreshToken) {
		
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("client_id", clientId);
		params.add("client_secret", clientSecret);
		params.add("grant_type", "refresh_token");
		params.add("refresh_token", refreshToken);
		try {
			HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, formUrlEncodedHeader);
			RestTemplate template = new RestTemplate();
			return template.postForObject(tokenEndpoint, tokenRequest, TokenResponse.class);
		} catch (RestClientResponseException e) {
			throw new AuthenticationException(e.getResponseBodyAsString());
		}
	}
	
	public TokenResponse passwordLogin(String userName, String password) {
		MultiValueMap<String, String> rqParams = new LinkedMultiValueMap<>();
		rqParams.add("client_id", clientId);
		rqParams.add("client_secret", clientSecret);
		rqParams.add("username", userName);
		rqParams.add("password", password);
		rqParams.add("grant_type", "password"); //https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.3.2

		TokenResponse tokens = null;
		try {
			HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(rqParams, formUrlEncodedHeader);
			RestTemplate template = new RestTemplate();
			tokens = template.postForObject(tokenEndpoint, tokenRequest, TokenResponse.class);
			if (tokens != null) log.debug(tokens.getId_token());
			if (tokens != null) log.debug(tokens.getAccess_token());
		} catch (RestClientResponseException e) {
			throw new AuthenticationException(e.getResponseBodyAsString());
		}
		if (tokens != null) tokens.accessClaims = keyResolver.checkToken(tokens.getAccess_token());
		if (tokens != null) tokens.idClaims = keyResolver.checkToken(tokens.getId_token());
		return tokens;
	}
	
	/**
	 * Extract the expiration date from a token.
	 * First tries to parse it as a JWT using KeyResolver.
	 * If that fails or if the token doesn't have an expiration claim,
	 * it will try to use the token introspection endpoint if configured.
	 *
	 * @param token The token to extract expiration from
	 * @return The expiration date or null if not available
	 */
	public Date getTokenExpiration(String token) {
		Date expiration = null;
		
		// First try to parse as JWT using KeyResolver
		if (keyResolver.isJwt(token)) {
			try {
				Claims claims = keyResolver.checkToken(token);
				if (claims.getExpiration() != null) {
					expiration = claims.getExpiration();
					log.debug("JWT token expiration: {}", expiration);
					return expiration;
				}
			} catch (RuntimeException e) {
				log.warn("Failed to extract expiration from JWT: {}", e.getMessage());
			}
		}
		
		// If not a JWT or couldn't extract expiration, try token introspection if configured
		var introspectionResult = callIntrospection(token);
		if (introspectionResult != null && introspectionResult.containsKey("exp")) {
			Object expValue = introspectionResult.get("exp");
			if (expValue instanceof Number) {
				long expTimestamp = ((Number) expValue).longValue() * 1000; // Convert seconds to milliseconds
				expiration = new Date(expTimestamp);
				log.debug("Token introspection expiration: {}", expiration);
			}
		}
		
		return expiration;
	}
	
	public Map<String, String> getUserInfo(String accessToken) {
		
	    if (userInfoEndpoint == null || userInfoEndpoint.isBlank()) {
	        throw new IllegalStateException("Userinfo endpoint is not configured");
	    }
		
	    try {
	        HttpHeaders headers = new HttpHeaders();
	        headers.set("Authorization", "Bearer " + accessToken);

	        HttpEntity<Void> userInfoRequest = new HttpEntity<>(headers);

	        RestTemplate template = new RestTemplate();
	        ParameterizedTypeReference<Map<String, String>> typeRef = new ParameterizedTypeReference<>() {};
	        
	        Map<String, String> userInfo = template.exchange(
	            userInfoEndpoint, 
	            HttpMethod.GET, 
	            userInfoRequest,
	            typeRef
	        ).getBody();

	        if (userInfo == null) {
	            throw new AuthenticationException("Invalid token");
	        }
	        
	        return userInfo;
	    } catch (RestClientResponseException e) {
			if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
				log.info("User info endpoint is forbidden. Most likely a technical user. Using sub as userId and username");
				return null;
			} else {
				log.warn("Error Retrieving User Info", e);
				throw new AuthenticationException("Invalid token");
			}
		}
	}

	public Map<String, Object> callIntrospection(String token) {
		if (introspectionEndpoint != null && !introspectionEndpoint.isEmpty()) {
			try {
				// Call the token introspection endpoint
				MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
				formData.add("token", token);
				formData.add("client_id", clientId);
				formData.add("client_secret", clientSecret);
				
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				
				HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
				RestTemplate restTemplate = new RestTemplate();
				
				ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
					introspectionEndpoint,
					HttpMethod.POST,
					request,
					new ParameterizedTypeReference<Map<String, Object>>() {}
				);
				
				Map<String, Object> introspectionResult = response.getBody();
				return introspectionResult;
			} catch (RuntimeException e) {
				log.error("Failed to introspect token: {}", e.getMessage());
				return null;
			}
		}
		return null;
	}
	
	private String hashString(String param) { //https://mkyong.com/java/java-sha-hashing-example/
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new SystemException(e);
		}
		byte[] hashInBytes = digest.digest(param.getBytes(StandardCharsets.UTF_8));
		
		// bytes to hex
		StringBuilder sb = new StringBuilder();
		for (byte b : hashInBytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
