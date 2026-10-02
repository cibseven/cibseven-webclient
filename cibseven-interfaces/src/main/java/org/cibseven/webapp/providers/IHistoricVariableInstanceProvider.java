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
package org.cibseven.webapp.providers;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.VariableHistory;

/**
 * Provider interface for historic variable instance operations
 */
public interface IHistoricVariableInstanceProvider {

	/**
	 * Retrieves a historic variable instance by its ID.
	 * @param id The ID of the historic variable instance
	 * @param deserializeValue Whether to deserialize the variable value or not
	 * @param user the user performing the search
	 * @return Historic variable instance details
	 * @throws SystemException in case of an error
	 * @throws NoObjectFoundException when the historic variable instance could not be found
	 */
	VariableHistory getHistoricVariableInstance(String id, boolean deserializeValue, CIBUser user) throws SystemException, NoObjectFoundException;

	/**
	 * Queries historic variable instances the way the engine does, with the filters in the body.
	 * @param filters The query as engine-rest takes it, empty for every instance
	 * @param firstResult Index of the first result to return
	 * @param maxResults Maximum number of results to return
	 * @param deserializeValues Whether the values are deserialized, left to the engine when null
	 * @param user the user performing the search
	 * @return the matching historic variable instances
	 * @throws SystemException in case of an error
	 */
	Collection<VariableHistory> findHistoricVariableInstances(Map<String, Object> filters,
			Optional<Integer> firstResult, Optional<Integer> maxResults, Boolean deserializeValues,
			CIBUser user) throws SystemException;

	/**
	 * Counts the historic variable instances a query matches.
	 * @param filters The query as engine-rest takes it, empty for every instance
	 * @param user the user performing the search
	 * @return how many instances match
	 * @throws SystemException in case of an error
	 */
	Integer findHistoricVariableInstancesCount(Map<String, Object> filters, CIBUser user) throws SystemException;

}
