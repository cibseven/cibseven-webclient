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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.cibseven.webapp.exception.SystemException;
import org.cibseven.modeler.model.FormUsageEntity;
import org.cibseven.modeler.repository.FormUsageRepository;

@Component
public class FormUsageProvider implements IFormUsageProvider {

	@Autowired
	private FormUsageRepository formDao;

	@Override
	public void delete(String id) throws SystemException {
		formDao.deleteById(id);
	}
	
	@Value("${cibsevenmodeler.session.expiresAfterMin:10}")
	private int sessionExpiresAfter;
	
	@Override
	public Optional<FormUsageEntity> getSessionById(String id) throws SystemException {
		return formDao.findById(id);
	}

	@Override
	public FormUsageEntity createFormUsage(FormUsageEntity entity) throws SystemException {
		entity.setOpenedAt(Timestamp.valueOf(LocalDateTime.now()));
		return formDao.save(entity);
	}

	@Override
	public FormUsageEntity closeSession(FormUsageEntity entity) throws SystemException {
		entity.setClosedAt(Timestamp.valueOf(LocalDateTime.now()));
		return formDao.save(entity);
	}
	
	@Override
	public FormUsageEntity findBySessionId(String sessionId) {
		return formDao.getBySessionId(sessionId);
	}
	
	@Override
	public FormUsageEntity checkSessionUser(String diagramId) {
		FormUsageEntity diagramUsage = formDao.checkSessionUser(diagramId);
		if (diagramUsage == null) return null;
		long openedAt = diagramUsage.getOpenedAt().getTime();
		long expirationTime = sessionExpiresAfter * 60 * 1000;
		long now = System.currentTimeMillis();
		long result = now - openedAt;
		if (result > expirationTime) {
			diagramUsage.setClosedAt(Timestamp.valueOf(LocalDateTime.now()));
			formDao.save(diagramUsage);
			return null; //session expired
		}
		return diagramUsage;
	}

}
