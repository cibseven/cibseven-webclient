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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

import org.cibseven.webapp.exception.SystemException;
import org.cibseven.modeler.model.FormEntity;
import org.cibseven.modeler.repository.FormRepository;

@Component
public class FormProvider implements IFormProvider {
	
	@Value("${cibsevenmodeler.deleteProcesses.versionLimit:50}") private Integer versionLimit;
	
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
	public FormEntity createForm(FormEntity entity) throws SystemException {
		entity.setCreated(LocalDateTime.now());
		entity.setUpdated(LocalDateTime.now());
		return formRepositoryDao.save(entity);
	}

	@Override
	public FormEntity updateForm(FormEntity entity) throws SystemException {
		FormEntity existing = formRepositoryDao.findById(entity.getId())
			.orElseThrow(() -> new EntityNotFoundException("FormEntity not found"));
		existing.setFormSchema(entity.getFormSchema());
		existing.setUpdated(LocalDateTime.now());
		existing.setUpdatedBy(entity.getUpdatedBy());
		return formRepositoryDao.save(existing);
	}
	
	@Transactional
	@Override
	public void delete(String id) throws SystemException {
		formRepositoryDao.deleteById(id);
	}

	@Override
	public List<FormEntity> getForms(int firstResult, int maxResults) throws SystemException {
		return formRepositoryDao.findAllBy(PageRequest.of(firstResult / maxResults, maxResults).withSort(Sort.by("updated").descending()));
	}
}
