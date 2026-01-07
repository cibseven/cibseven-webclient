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
package org.cibseven.webapp.providers;

import java.util.Arrays;
import java.util.Collection;

import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Engine;
import org.cibseven.webapp.rest.model.NewUser;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EngineProvider extends SevenProviderBase implements IEngineProvider {

	@Override
	public Collection<Engine> getProcessEngineNames() {
		String url = getEngineRestUrl() + "/engine";
		
		return Arrays.asList(
			((ResponseEntity<Engine[]>) doGet(url, Engine[].class, null, false)).getBody()
		);
	}

	@Override
	public Boolean requiresSetup(String engine) {
		String url = getEngineRestUrl();
		if (engine != null && !engine.isEmpty() && !"default".equals(engine)) {
		url+= "/engine/" + engine;
	  }
		url+=  "/setup/status";
		return ((ResponseEntity<Boolean>) doGet(url, Boolean.class, null, false)).getBody();
	}

	@Override
	public void createSetupUser(NewUser user, String engine) throws InvalidUserIdException {
		String url = getEngineRestUrl();
		if (engine != null && !engine.isEmpty() && !"default".equals(engine)) {
		url+= "/engine/" + engine;
		}
		url+=  "/setup/user/create";
		try {
			//	A JSON object with the following properties:
			//	Name 	Type 	Description
			//	profile 	Array 	A JSON object containing variable key-value pairs. The object contains the following properties: id (String), firstName (String), lastName (String) and email (String).
			//	credentials 	Array 	A JSON object containing variable key-value pairs. The object contains the following property: password (String). 	 * 

			String body = "{\"profile\":"
					+ user.getProfile().json()
					+ ",\"credentials\":"
					+ user.getCredentials().json()
					+ "}";

			doPost(url, body , null, null);

		} catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in createUser(...):", se);
			throw se;
		}

	}
}
