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

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.ExternalTask;

/**
 * Interface for external task operations
 */
public interface IExternalTaskProvider {

	/**
	 * Get external tasks based on query parameters
	 * 
	 * @param queryParams Query parameters for filtering external tasks
	 * @param user the user performing the operation
	 * @return Collection of external tasks
	 */
	Collection<ExternalTask> getExternalTasks(Map<String, Object> queryParams, CIBUser user);

}