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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.cibseven.bpm.BpmPlatform;
import org.cibseven.bpm.engine.rest.dto.identity.UserCredentialsDto;
import org.cibseven.bpm.engine.rest.dto.identity.UserDto;
import org.cibseven.bpm.engine.rest.dto.identity.UserProfileDto;
import org.cibseven.bpm.engine.rest.impl.SetupRestServiceImpl;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Engine;
import org.cibseven.webapp.rest.model.NewUser;

public class DirectEngineProvider implements IEngineProvider {

	DirectProviderUtil directProviderUtil;

	DirectEngineProvider(DirectProviderUtil directProviderUtil){
		this.directProviderUtil = directProviderUtil;
	}

	@Override
	public Collection<Engine> getProcessEngineNames() {
		Set<String> engineNames = BpmPlatform.getProcessEngineService().getProcessEngineNames();
		List<Engine> results = new ArrayList<>();
		for (String engineName : engineNames) {
			results.add(new Engine(engineName));
		}

		return results;
	}

	public Boolean requiresSetup(String engine) {
		return new SetupRestServiceImpl(engine, directProviderUtil.getObjectMapper(engine)).requiresSetup();
	}
	
	public void createSetupUser(NewUser user, String engine) throws InvalidUserIdException {
		UserDto userDto = new UserDto();
		UserProfileDto profileDto = new UserProfileDto();
		if (user.getProfile() == null || user.getCredentials() == null) 
			throw new SystemException("User data not provided."); 
		profileDto.setEmail(user.getProfile().getEmail());
		profileDto.setFirstName(user.getProfile().getFirstName());
		profileDto.setId(user.getProfile().getId());
		profileDto.setLastName(user.getProfile().getLastName());
		userDto.setProfile(profileDto);
		UserCredentialsDto userCredentialsDto = new UserCredentialsDto();
		userCredentialsDto.setPassword(user.getCredentials().getPassword());
		userDto.setCredentials(userCredentialsDto);
		new SetupRestServiceImpl(engine, directProviderUtil.getObjectMapper(engine)).createUser(userDto);
	}

}
