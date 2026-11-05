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

import jakarta.persistence.EntityNotFoundException;

import org.cibseven.webapp.exception.ExistingFormIdException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.ValueTooLongException;
import org.cibseven.modeler.model.FormEntity;
import org.cibseven.modeler.repository.FormRepository;

@Component
public class FormProvider implements IFormProvider {
	
	@Autowired
	private FormRepository formRepositoryDao;
	

	@Override
	public List<FormEntity> getForms(String keyword, int firstResult, int maxResults) throws SystemException {
		return getForms(keyword, ProjectGuard.HiddenProjects.NONE, firstResult, maxResults);
	}

	/** Leaves out the forms of the hidden projects, counting only what is left for the paging. */
	public List<FormEntity> getForms(String keyword, ProjectGuard.HiddenProjects hidden, int firstResult, int maxResults)
			throws SystemException {
		String kw = keyword == null ? "" : keyword;
		return ProjectGuard.visiblePage(hidden, firstResult, maxResults,
			(hiddenProjectIds, page, size) -> formRepositoryDao.findAllFiltered(kw, hiddenProjectIds,
				PageRequest.of(page, size, Sort.by("updated").descending())),
			FormEntity::getFolderId);
	}

	@Override
	public Optional<FormEntity> findById(String id) throws SystemException {
		return formRepositoryDao.findById(id);
	}

	/** The latest of the forms with this form id, whichever folder it is in. */
	@Override
	public FormEntity findByFormId(String formId) throws SystemException {
		return formRepositoryDao.findByFormIdOrderByUpdatedDesc(formId).stream().findFirst().orElse(null);
	}

	/** The forms with this form id the user may see, the latest change first. */
	public List<FormEntity> findByFormId(String formId, ProjectGuard.HiddenProjects hidden) throws SystemException {
		return formRepositoryDao.findByFormIdOrderByUpdatedDesc(formId).stream()
			.filter(form -> !hidden.hides(form.getFolderId()))
			.toList();
	}

	public FormEntity findByFormIdInFolder(String formId, String folderId) throws SystemException {
		return formRepositoryDao.findByFormIdAndFolderId(formId, folderId);
	}

	/**
	 * Whether a form other than this one has the form id, in any folder and any project. The engine
	 * resolves a form reference by it, so deploying either adds a version to what the other deployed.
	 */
	public boolean isFormIdUsedElsewhere(String formId, String id) throws SystemException {
		return id == null ? formRepositoryDao.existsByFormId(formId) : formRepositoryDao.existsByFormIdAndIdNot(formId, id);
	}

	@Override
	public FormEntity createForm(FormEntity entity) throws SystemException {
		requireFits(entity);
		requireFormIdFree(entity.getFormId(), entity.getFolderId(), null);
		entity.setCreated(Timestamp.valueOf(LocalDateTime.now()));
		entity.setUpdated(Timestamp.valueOf(LocalDateTime.now()));
		return store(entity);
	}

	@Override
	public FormEntity updateForm(FormEntity entity) throws SystemException {
		FormEntity stored = formRepositoryDao.findById(entity.getId())
			.orElseThrow(() -> new EntityNotFoundException("FormEntity not found"));
		applyUpdate(entity, stored);
		return store(stored);
	}

	/**
	 * Writes at once, so a form id the database refuses is refused here as the request error. The
	 * check of the folder passed, so another save took it meanwhile, or the schema is one from before
	 * 3.0, where a form id is unique across all folders. Left to the commit, it fails as a system
	 * error carrying the SQL.
	 */
	private FormEntity store(FormEntity entity) {
		try {
			FormEntity saved = formRepositoryDao.save(entity);
			formRepositoryDao.flush();
			return saved;
		} catch (DataIntegrityViolationException e) {
			if (entity.getFormId() == null) {
				throw e;
			}
			throw new ExistingFormIdException(entity.getFormId());
		}
	}

	/**
	 * Copies what an update may change onto the stored form, leaving the rest as it is. A
	 * subclass that writes the row itself calls this instead of repeating the list, so a field
	 * added here reaches it too.
	 */
	protected void applyUpdate(FormEntity source, FormEntity stored) {
		requireFits(source);
		// An update keeps the form id, so only a move can bring it into a folder that has it
		requireFormIdFree(stored.getFormId(), source.getFolderId() != null ? source.getFolderId() : stored.getFolderId(),
			stored.getId());
		stored.setFormSchema(source.getFormSchema());
		// Only when one is named: an import that replaces the content carries no folder and
		// has to leave the form in the one it is already in
		if (source.getFolderId() != null) {
			stored.setFolderId(source.getFolderId());
		}
		stored.setUpdated(Timestamp.valueOf(LocalDateTime.now()));
		stored.setUpdatedBy(source.getUpdatedBy());
	}
	
	/**
	 * Every write goes through here, so a value the column cannot hold is refused as a request
	 * error naming the field. Left to the database it surfaces as a system error carrying the
	 * failed SQL, which tells the user nothing and says more than it should.
	 */
	private void requireFits(FormEntity entity) {
		requireFits("formId", entity.getFormId(), 100);
		requireFits("description", entity.getDescription(), 150);
	}

	/**
	 * A form id is unique within a folder. Refused here as the request error the user can act on,
	 * rather than left to the database constraint, which fails as a system error.
	 */
	private void requireFormIdFree(String formId, String folderId, String id) {
		if (formId == null || folderId == null) {
			return;
		}
		FormEntity holder = formRepositoryDao.findByFormIdAndFolderId(formId, folderId);
		if (holder != null && !holder.getId().equals(id)) {
			throw new ExistingFormIdException(formId);
		}
	}

	private void requireFits(String field, String value, int limit) {
		if (value != null && value.length() > limit) {
			throw new ValueTooLongException(field, limit);
		}
	}

	@Transactional(CibsevenJpa.TRANSACTION_MANAGER)
	@Override
	public void delete(String id) throws SystemException {
		formRepositoryDao.deleteById(id);
	}

	@Override
	public List<FormEntity> getForms(int firstResult, int maxResults) throws SystemException {
		return formRepositoryDao.findAllBy(PageRequest.of(firstResult / maxResults, maxResults).withSort(Sort.by("updated").descending()));
	}
}
