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
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.modeler.model.DiagramUsageEntity;
import org.cibseven.modeler.repository.DiagramUsageRepository;
import org.cibseven.modeler.repository.UserSessionRepository;

@Component
public class DiagramUsageProvider implements IDiagramUsageProvider {

	@Value("${cibsevenmodeler.deleteUsages.usageLimit:50}") private Integer usageLimit;
	
	@Autowired
	private DiagramUsageRepository processDiagramDao;
	
	@Autowired
	private UserSessionRepository userSessionRepository;

	@Override
	public void delete(String id) throws SystemException {
		processDiagramDao.deleteById(id);
	}
	
	@Scheduled(cron = "${cibsevenmodeler.deleteUsages.cron: 0 0 0 * * ?}")
	void removeOldVersions() {		
		userSessionRepository.removeOldSessions(usageLimit);
	}
	
	@Value("${cibsevenmodeler.session.expiresAfterMin:10}")
	private int sessionExpiresAfter;
	
	@Override
	public Optional<DiagramUsageEntity> getSessionById(String id) throws SystemException {
		return processDiagramDao.findById(id);
	}

	@Override
	public DiagramUsageEntity createDiagramUsage(DiagramUsageEntity entity) throws SystemException {
		entity.setOpenedAt(LocalDateTime.now());
		return processDiagramDao.save(entity);
	}

	@Override
	public DiagramUsageEntity closeSession(DiagramUsageEntity entity) throws SystemException {
		entity.setClosedAt(LocalDateTime.now());
		return processDiagramDao.save(entity);
	}
	
	@Override
	public DiagramUsageEntity findBySessionId(String sessionId) {
		return processDiagramDao.getBySessionId(sessionId);
	}
	
	@Override
	public DiagramUsageEntity checkSessionUser(String diagramId) {
	    List<DiagramUsageEntity> diagramUsages = processDiagramDao.checkSessionUser(diagramId);

	    if (diagramUsages.isEmpty()) {
	        return null;
	    }

	    for (DiagramUsageEntity diagramUsage : diagramUsages) {
	        LocalDateTime expirationTime = diagramUsage.getOpenedAt().plusMinutes(sessionExpiresAfter);

	        if (LocalDateTime.now().isAfter(expirationTime)) {
	            diagramUsage.setClosedAt(LocalDateTime.now());
	            processDiagramDao.save(diagramUsage);
	        }
	    }

	    return diagramUsages.stream()
	    	.filter(d -> d.getClosedAt() == null)
	    	.findFirst()
	    	.orElse(null);
	}

}
