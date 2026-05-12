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
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cibseven.webapp.exception.SystemException;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.model.ProcessDiagramReduce;
import org.cibseven.modeler.repository.ProcessDiagramRepository;
import jakarta.persistence.EntityNotFoundException;

@Component
public class DBProcessDiagramProvider implements IProcessDiagramProvider {
	
	@Autowired
	private ProcessDiagramRepository processDiagramDao;

	@Override
	public List<ProcessDiagramReduce> getDiagrams(String keyword, String diagramType, int firstResult, int maxResults) throws SystemException {
		String kw = keyword == null ? "" : keyword;
		String dt = diagramType == null ? "" : diagramType;
		PageRequest page = PageRequest.of(firstResult / maxResults, maxResults, Sort.by("updated").descending());
		return processDiagramDao.findAllFiltered(kw, dt, page);
	}

	@Override
	public List<ProcessDiagramReduce> getDiagrams(int firstResult, int maxResults) throws SystemException {
		return processDiagramDao.findAllBy(PageRequest.of(firstResult / maxResults, maxResults).withSort(Sort.by("updated").descending()));
	}

	@Override
	public Optional<ProcessDiagramEntity> findById(String id) throws SystemException {
		return processDiagramDao.findById(id);
	}

	@Override
	public ProcessDiagramEntity findByName(String name) throws SystemException {
		return processDiagramDao.findByName(name);
	}
	
	@Override
	public ProcessDiagramEntity findByProcessKey(String key) throws SystemException {
		return processDiagramDao.findByProcesskey(key);
	}

	@Override
	public ProcessDiagramEntity createDiagram(ProcessDiagramEntity entity) throws SystemException {
		entity.setCreated(Timestamp.valueOf(LocalDateTime.now()));
		entity.setUpdated(Timestamp.valueOf(LocalDateTime.now()));
		return processDiagramDao.save(entity);
	}

	@Override
	public ProcessDiagramEntity updateDiagram(ProcessDiagramEntity entity) throws SystemException {
		ProcessDiagramEntity processDiagramEntity = processDiagramDao.findById(entity.getId()).orElseThrow(() -> new EntityNotFoundException("ProcessDiagramEntity not found"));
		processDiagramEntity.setName(entity.getName());
		processDiagramEntity.setProcesskey(entity.getProcesskey());
		processDiagramEntity.setDescription(entity.getDescription());
		processDiagramEntity.setType(entity.getType());
		processDiagramEntity.setDiagram(entity.getDiagram());
		processDiagramEntity.setUpdated(Timestamp.valueOf(LocalDateTime.now()));
		processDiagramEntity.setUpdatedBy(entity.getUpdatedBy());

		return processDiagramDao.save(processDiagramEntity);
	}
	
	@Transactional
	@Override
	public void delete(String id) throws SystemException {
		processDiagramDao.deleteById(id);
	}

}
