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
package org.cibseven.modeler.provider;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.cibseven.webapp.exception.SystemException;
import org.cibseven.modeler.model.UserSessionEntity;
import org.cibseven.modeler.repository.UserSessionRepository;

@Component
public class UserSessionProvider implements IUserSessionProvider {

	@Autowired
	private UserSessionRepository userSessionRepository;
	
	@Override
	public UserSessionEntity createSession(UserSessionEntity entity) throws SystemException {
		entity.setCreatedAt(LocalDateTime.now());
		return userSessionRepository.save(entity);
	}
	
	@Override
	public void delete(String id) throws SystemException {
		userSessionRepository.deleteById(id);
	}
	

}
