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
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.cibseven.webapp.exception.SystemException;
import org.cibseven.modeler.model.ElementTemplate;
import org.cibseven.modeler.repository.ElementTemplateRepository;

/**
 * Provider class for Element Template data access operations.
 * <p>
 * This class serves as a service layer component that handles all database
 * operations for Element Templates. It implements the IElementTemplateProvider
 * interface and provides CRUD operations, partial updates, and bulk operations
 * for Element Template entities.
 * </p>
 * <p>
 * The provider abstracts the direct repository access and can be extended
 * with additional business logic, caching, or validation as needed.
 * </p>
 * 
 * @author Flow Modeler Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class ElementTemplateProvider implements IElementTemplateProvider {

	@Autowired
	private ElementTemplateRepository repository;
	
	/**
	 * Retrieves all element templates from the database.
	 * 
	 * @return list of all element templates
	 * @throws SystemException if a database error occurs
	 */
	@Override
	public List<ElementTemplate> getElementTemplates() throws SystemException {
		return repository.findAll();
	}

	/**
	 * Finds an element template by its unique identifier.
	 * 
	 * @param id the unique identifier of the template
	 * @return Optional containing the template if found, empty otherwise
	 * @throws SystemException if a database error occurs
	 */
	@Override
	public Optional<ElementTemplate> findById(String id) throws SystemException {
		return repository.findById(id);
	}

	/**
	 * Adds a new element template to the database.
	 * 
	 * @param template the template to add
	 * @return the saved template with generated ID and timestamps
	 * @throws SystemException if a database error occurs
	 */
	@Override
	public ElementTemplate addTemplate(ElementTemplate template) throws SystemException {
		return repository.save(template);
	}
	
	/**
	 * Deletes an element template by its unique identifier.
	 * 
	 * @param id the unique identifier of the template to delete
	 * @throws SystemException if a database error occurs
	 */
	public void deleteTemplateById(String id) throws SystemException {
		repository.deleteById(id);
	}
	
	/**
	 * Performs a partial update of an element template.
	 * <p>
	 * Only updates the properties specified in the properties map.
	 * Currently supports updating the 'active' property.
	 * </p>
	 * 
	 * @param template the template to update
	 * @param properties map of properties to update
	 * @return Optional containing the updated template if changes were made, empty otherwise
	 */
	@Override
	public Optional<ElementTemplate> partialUpdate(ElementTemplate template, Map<String, Object> properties) {
		boolean modified = false;
    	
    	if (properties.containsKey("active")) {
    		Object value = properties.get("active");
    		
    		if (value instanceof Boolean && template.getActive() != value) {
    			template.setActive((Boolean) value);
    			modified = true;
    		}
    	}
    	
    	if (modified) {
    		return Optional.of(repository.save(template));
    	}
    	
    	return Optional.empty();
	}

	/**
	 * Performs a full update of an element template.
	 * 
	 * @param id the unique identifier of the template to update
	 * @param newData the new template data
	 * @return the updated template
	 */
	@Override
	public ElementTemplate updateTemplate(String id, ElementTemplate newData) {
		return repository.save(newData);
	}
	
	/**
	 * Adds multiple element templates in bulk.
	 * 
	 * @param templatesToAdd list of templates to add
	 * @return list of saved templates
	 * @throws SystemException if a database error occurs
	 */
	@Override
	public List<ElementTemplate> addAll(List<ElementTemplate> templatesToAdd) throws SystemException {
		return repository.saveAll(templatesToAdd);
	}
}
