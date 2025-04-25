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
	
	private void loadKey(Optional<String> keyId) {
		RestTemplate rest = new RestTemplate();
		KeyList list = rest.getForObject(certEndpoint, KeyList.class);
		if (keyId.isPresent())
			keyData = Stream.of(list.getKeys()).filter(a -> a.getKid().equals(keyId.get())).findFirst()
				.orElseThrow(() -> new AuthenticationException("Missing Public key"));
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
		if(!header.getKeyId().equals(keyData.kid)) {
			log.info("Wrong key, loading new, {}", System.currentTimeMillis());
			try {
				loadKey(Optional.of(header.getKeyId()));
			} catch (AuthenticationException e) {
				throw new RuntimeException("Couldn't load key with kid " + header.getKeyId() + " from " + certEndpoint);
			}
			log.info("New key loaded, {}", System.currentTimeMillis());
		}
		return key;
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