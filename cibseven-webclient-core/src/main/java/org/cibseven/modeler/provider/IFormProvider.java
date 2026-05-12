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

import java.util.List;
import java.util.Optional;

import org.cibseven.webapp.exception.SystemException;
import org.cibseven.modeler.model.FormEntity;

public interface IFormProvider {

	/**
	 * Get all forms with optional keyword filter.
	 */
	List<FormEntity> getForms(String keyword, int firstResult, int maxResults) throws SystemException;

	/**
	 * Get all forms without filters.
	 */
	List<FormEntity> getForms(int firstResult, int maxResults) throws SystemException;
	
	/**
	 * Find form by id
	 * 
	 * @param id
	 */
	Optional<FormEntity> findById(String id) throws SystemException;
    
    /**
	 * Create a new form.
	 * 
	 * @param entity
	 */
	FormEntity createForm(FormEntity entity) throws SystemException;    

	 /**
	 * Deletes a form.
	 * 
	 * @param entity
	 */    
	void delete(String id) throws SystemException;
	
    /**
	 * Update a form.
	 * 
	 * @param entity
	 */
	FormEntity updateForm(FormEntity entity) throws SystemException;
	

	
	
}
