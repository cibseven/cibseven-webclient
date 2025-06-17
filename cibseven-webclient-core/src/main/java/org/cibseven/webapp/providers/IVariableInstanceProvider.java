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

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.VariableInstance;

/**
 * Provider interface for variable instance operations
 */
public interface IVariableInstanceProvider {

	/**
	 * Retrieves a variable instance by its ID.
	 * @param id The ID of the variable instance
	 * @param deserializeValue Whether to deserialize the variable value or not
	 * @param user the user performing the search
	 * @return Variable instance details
	 * @throws SystemException in case of an error
	 * @throws NoObjectFoundException when the variable instance could not be found
	 */
	VariableInstance getVariableInstance(String id, Boolean deserializeValue, CIBUser user) throws SystemException, NoObjectFoundException;

}
