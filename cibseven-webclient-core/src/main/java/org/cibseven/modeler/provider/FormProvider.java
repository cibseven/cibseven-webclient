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
import org.cibseven.webapp.persistence.CibsevenJpa;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

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
		String kw = keyword == null ? "" : keyword;
		PageRequest page = PageRequest.of(firstResult / maxResults, maxResults, Sort.by("updated").descending());
		return formRepositoryDao.findAllFiltered(kw, page);
	}

	@Override
	public Optional<FormEntity> findById(String id) throws SystemException {
		return formRepositoryDao.findById(id);
	}

	@Override
	public FormEntity findByFormId(String formId) throws SystemException {
		return formRepositoryDao.findByFormId(formId);
	}
	
	@Override
	public FormEntity createForm(FormEntity entity) throws SystemException {
		requireFits(entity);
		entity.setCreated(Timestamp.valueOf(LocalDateTime.now()));
		entity.setUpdated(Timestamp.valueOf(LocalDateTime.now()));
		return formRepositoryDao.save(entity);
	}

	@Override
	public FormEntity updateForm(FormEntity entity) throws SystemException {
		FormEntity stored = formRepositoryDao.findById(entity.getId())
			.orElseThrow(() -> new EntityNotFoundException("FormEntity not found"));
		applyUpdate(entity, stored);
		return formRepositoryDao.save(stored);
	}

	/**
	 * Copies what an update may change onto the stored form, leaving the rest as it is. A
	 * subclass that writes the row itself calls this instead of repeating the list, so a field
	 * added here reaches it too.
	 */
	protected void applyUpdate(FormEntity source, FormEntity stored) {
		requireFits(source);
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
