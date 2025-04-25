/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cibseven.webapp.auth;

import java.time.Duration;

import org.cibseven.webapp.auth.providers.JwtUserProvider.TokenSettings;

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
