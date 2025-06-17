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
import org.springframework.stereotype.Component;

/**
 * Provider implementation for variable instance operations using Camunda REST API
 */
@Component
public class VariableInstanceProvider extends SevenProviderBase implements IVariableInstanceProvider {

	@Override
	public VariableInstance getVariableInstance(String id, Boolean deserializeValue, CIBUser user) throws SystemException, NoObjectFoundException {
		String url = getEngineRestUrl() + "/variable-instance/" + id;
		if (deserializeValue != null) {
			url += "?deserializeValue=" + deserializeValue;
		}
		return doGet(url, VariableInstance.class, user, false).getBody();
	}

}
