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

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;

import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JweHeader;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.ProtectedHeader;
import io.jsonwebtoken.impl.lang.Function;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter @NoArgsConstructor
class KeyData {
	String alg;
	String kty;
	String use;
	String[] x5c;
	String n;
	String e;
	String kid;
	String x5t;
	@JsonProperty("x5t#S256")
	String x5t256;
}

@Getter @Setter @NoArgsConstructor
class KeyList {
	KeyData[] keys;
}

@Slf4j
public class KeyResolver extends LocatorAdapter<Key> implements Function<Header, Key> {

	String certEndpoint;
	KeyData keyData;
	@Getter
	PublicKey key;
	
	JwtParser parser;
	

	public KeyResolver(String certEndpoint) {
		this.certEndpoint = certEndpoint;
		parser = Jwts.parser().keyLocator(this).build();
		loadKey(Optional.empty());
	}
	
	private void loadKey(Optional<KeyMatcher> matcher) {
		RestTemplate rest = new RestTemplate();
		KeyList list = rest.getForObject(certEndpoint, KeyList.class);
		if (list == null)
			throw new AuthenticationException("Missing KeyList");
		if (matcher.isPresent())
			keyData = Stream.of(list.getKeys()).filter(matcher.get()::matches).findFirst()
				.orElseThrow(() -> new AuthenticationException("Missing public key for " + matcher.get()));
		else keyData = list.getKeys()[0];
		key = convertToPublicKey(new BigInteger(1, Base64.getUrlDecoder().decode(keyData.getN())),
				new BigInteger(1, Base64.getUrlDecoder().decode(keyData.getE())));
	}
	
	public Claims checkToken(String token) throws AuthenticationException {
		try {
			return parser.parseSignedClaims(token).getPayload();
		} catch (JwtException e) {
			throw new AuthenticationException("Failure while parsing sso token");
		}
	}
	
	@Override
	protected Key locate(JwsHeader header) {
		return resolveKey(header);
	}
	
	@Override
	protected Key locate(JweHeader header) {
		return resolveKey(header);
	}
	
	public boolean isJwt(String token) {
		try {
			parser.parse(token);
            return true;
        } catch (MalformedJwtException e) {
            return false;
        }
	}
	
	private Key resolveKey(ProtectedHeader header) {
		KeyMatcher matcher = matcherFor(header);
		if (!matcher.matches(keyData)) {
			log.info("Wrong key, loading new, {}", System.currentTimeMillis());
			try {
				loadKey(Optional.of(matcher));
			} catch (AuthenticationException e) {
				throw new RuntimeException("Couldn't load key (" + matcher + ") from " + certEndpoint);
			}
			log.info("New key loaded, {}", System.currentTimeMillis());
		}
		return key;
	}

	// The token header identifies its signing key via kid, x5t (SHA-1) or x5t#S256 (SHA-256),
	// any of which is optional. Pick whichever is present; fail if the header carries none.
	private KeyMatcher matcherFor(ProtectedHeader header) {
		if (header.getKeyId() != null)
			return new KeyMatcher("kid", header.getKeyId(), KeyData::getKid);
		String x5t = encodeThumbprint(header.getX509Sha1Thumbprint());
		if (x5t != null)
			return new KeyMatcher("x5t", x5t, KeyData::getX5t);
		String x5t256 = encodeThumbprint(header.getX509Sha256Thumbprint());
		if (x5t256 != null)
			return new KeyMatcher("x5t#S256", x5t256, KeyData::getX5t256);
		throw new AuthenticationException("Token header carries no key identifier (kid, x5t or x5t#S256)");
	}

	private static String encodeThumbprint(byte[] thumbprint) {
		return thumbprint == null ? null : Base64.getUrlEncoder().withoutPadding().encodeToString(thumbprint);
	}

	// Matches a JWKS entry by a single identifier (kid/x5t/x5t#S256) taken from the token header.
	private static final class KeyMatcher {
		private final String label;
		private final String value;
		private final java.util.function.Function<KeyData, String> extractor;

		KeyMatcher(String label, String value, java.util.function.Function<KeyData, String> extractor) {
			this.label = label;
			this.value = value;
			this.extractor = extractor;
		}

		boolean matches(KeyData candidate) {
			return value.equals(extractor.apply(candidate));
		}

		@Override
		public String toString() {
			return label + "=" + value;
		}
	}
	
	public PublicKey convertToPublicKey(BigInteger modulus, BigInteger publicExponent) {
	    try {
	    	RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, publicExponent);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			return kf.generatePublic(spec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException("Failed to convert key to public key", e);
		}
	}

	@Override
	public Key apply(Header header) {
		return locate(header);
	}
}