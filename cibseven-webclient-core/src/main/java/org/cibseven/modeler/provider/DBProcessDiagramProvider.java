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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.cibseven.persistence.CibsevenJpa;
import org.springframework.transaction.annotation.Transactional;

import org.cibseven.webapp.exception.ExistingProcessKeyException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.ValueTooLongException;
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
		return getDiagrams(keyword, diagramType, ProjectGuard.HiddenProjects.NONE, firstResult, maxResults);
	}

	/** Leaves out the diagrams of the hidden projects, counting only what is left for the paging. */
	public List<ProcessDiagramReduce> getDiagrams(String keyword, String diagramType, ProjectGuard.HiddenProjects hidden,
			int firstResult, int maxResults) throws SystemException {
		String kw = keyword == null ? "" : keyword;
		String dt = diagramType == null ? "" : diagramType;
		return ProjectGuard.visiblePage(hidden, firstResult, maxResults,
			(hiddenProjectIds, page, size) -> processDiagramDao.findAllFiltered(kw, dt, hiddenProjectIds,
				PageRequest.of(page, size, Sort.by("updated").descending())),
			ProcessDiagramReduce::getFolderId);
	}

	@Override
	public List<ProcessDiagramReduce> getDiagrams(int firstResult, int maxResults) throws SystemException {
		return processDiagramDao.findAllBy(PageRequest.of(firstResult / maxResults, maxResults).withSort(Sort.by("updated").descending()));
	}

	@Override
	public Optional<ProcessDiagramEntity> findById(String id) throws SystemException {
		return processDiagramDao.findById(id);
	}

	/** The latest of the diagrams with this name, whichever folder it is in. */
	@Override
	public ProcessDiagramEntity findByName(String name) throws SystemException {
		return processDiagramDao.findByNameOrderByUpdatedDesc(name).stream().findFirst().orElse(null);
	}

	/** The latest of the diagrams with this name the user may see. */
	public ProcessDiagramEntity findByName(String name, ProjectGuard.HiddenProjects hidden) throws SystemException {
		return processDiagramDao.findByNameOrderByUpdatedDesc(name).stream()
			.filter(diagram -> !hidden.hides(diagram.getFolderId()))
			.findFirst().orElse(null);
	}

	/** The latest of the diagrams with this key, whichever folder it is in. */
	@Override
	public ProcessDiagramEntity findByProcessKey(String key) throws SystemException {
		return processDiagramDao.findByProcesskeyOrderByUpdatedDesc(key).stream().findFirst().orElse(null);
	}

	/** The diagrams with this key the user may see, the latest change first. */
	public List<ProcessDiagramEntity> findByProcessKey(String key, ProjectGuard.HiddenProjects hidden) throws SystemException {
		return processDiagramDao.findByProcesskeyOrderByUpdatedDesc(key).stream()
			.filter(diagram -> !hidden.hides(diagram.getFolderId()))
			.toList();
	}

	public ProcessDiagramEntity findByProcessKeyInFolder(String key, String folderId) throws SystemException {
		return processDiagramDao.findByProcesskeyAndFolderId(key, folderId);
	}

	/**
	 * Whether a diagram other than this one has the key, in any folder and any project. The engine
	 * keeps one definition per key, so deploying either adds a version to what the other deployed.
	 */
	public boolean isKeyUsedElsewhere(String key, String id) throws SystemException {
		return id == null ? processDiagramDao.existsByProcesskey(key) : processDiagramDao.existsByProcesskeyAndIdNot(key, id);
	}

	@Override
	public ProcessDiagramEntity createDiagram(ProcessDiagramEntity entity) throws SystemException {
		requireFits(entity);
		requireKeyFree(entity.getProcesskey(), entity.getFolderId(), null);
		entity.setCreated(Timestamp.valueOf(LocalDateTime.now()));
		entity.setUpdated(Timestamp.valueOf(LocalDateTime.now()));
		return store(entity);
	}

	/**
	 * Writes at once, so a key the database refuses is refused here as the request error. The check
	 * of the folder passed, so another save took the key meanwhile, or the schema is one from before
	 * 3.0, where a key is unique across all folders. Left to the commit, it fails as a system error
	 * carrying the SQL.
	 */
	private ProcessDiagramEntity store(ProcessDiagramEntity entity) {
		try {
			ProcessDiagramEntity saved = processDiagramDao.save(entity);
			processDiagramDao.flush();
			return saved;
		} catch (DataIntegrityViolationException e) {
			if (entity.getProcesskey() == null) {
				throw e;
			}
			throw new ExistingProcessKeyException(entity.getProcesskey());
		}
	}

	/**
	 * Every write goes through here, so a value the column cannot hold is refused as a request
	 * error naming the field. Left to the database it surfaces as a system error carrying the
	 * failed SQL, which tells the user nothing and says more than it should.
	 */
	private void requireFits(ProcessDiagramEntity entity) {
		requireFits("name", entity.getName(), 255);
		requireFits("processkey", entity.getProcesskey(), 100);
		requireFits("description", entity.getDescription(), 150);
	}

	/**
	 * A key is unique within a folder. Refused here as the request error the user can act on,
	 * rather than left to the database constraint, which fails as a system error.
	 */
	private void requireKeyFree(String key, String folderId, String id) {
		if (key == null || folderId == null) {
			return;
		}
		ProcessDiagramEntity holder = processDiagramDao.findByProcesskeyAndFolderId(key, folderId);
		if (holder != null && !holder.getId().equals(id)) {
			throw new ExistingProcessKeyException(key);
		}
	}

	private void requireFits(String field, String value, int limit) {
		if (value != null && value.length() > limit) {
			throw new ValueTooLongException(field, limit);
		}
	}

	@Override
	public ProcessDiagramEntity updateDiagram(ProcessDiagramEntity entity) throws SystemException {
		ProcessDiagramEntity stored = processDiagramDao.findById(entity.getId()).orElseThrow(() -> new EntityNotFoundException("ProcessDiagramEntity not found"));
		applyUpdate(entity, stored);
		return store(stored);
	}

	/**
	 * Copies what an update may change onto the stored diagram, leaving the rest as it is. A
	 * subclass that writes the row itself calls this instead of repeating the list, so a field
	 * added here reaches it too.
	 */
	protected void applyUpdate(ProcessDiagramEntity source, ProcessDiagramEntity stored) {
		requireFits(source);
		requireKeyFree(source.getProcesskey(), source.getFolderId() != null ? source.getFolderId() : stored.getFolderId(),
			stored.getId());
		stored.setName(source.getName());
		stored.setProcesskey(source.getProcesskey());
		stored.setDescription(source.getDescription());
		stored.setType(source.getType());
		stored.setDiagram(source.getDiagram());
		// Only when one is named: an import that replaces the content carries no folder and
		// has to leave the diagram in the one it is already in
		if (source.getFolderId() != null) {
			stored.setFolderId(source.getFolderId());
		}
		stored.setUpdated(Timestamp.valueOf(LocalDateTime.now()));
		stored.setUpdatedBy(source.getUpdatedBy());
	}
	
	@Transactional(CibsevenJpa.TRANSACTION_MANAGER)
	@Override
	public void delete(String id) throws SystemException {
		processDiagramDao.deleteById(id);
	}

}
