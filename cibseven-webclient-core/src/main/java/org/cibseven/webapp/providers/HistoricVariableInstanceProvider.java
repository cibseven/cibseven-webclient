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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.cibseven.webapp.providers.utils.URLUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Provider implementation for historic variable instance operations using Camunda History REST API
 */
@Component
public class HistoricVariableInstanceProvider extends SevenProviderBase implements IHistoricVariableInstanceProvider {

	private VariableHistory getHistoricVariableInstanceImpl(String id, boolean deserializeValue, CIBUser user) throws SystemException, NoObjectFoundException {
		String url = getEngineRestUrl(user) + "/history/variable-instance/" + id +
			"?deserializeValue=" + deserializeValue;
		return doGet(url, VariableHistory.class, user, false).getBody();
	}

	@Override
	public VariableHistory getHistoricVariableInstance(String id, boolean deserializeValue, CIBUser user) throws SystemException, NoObjectFoundException {
		VariableHistory variableSerialized = getHistoricVariableInstanceImpl(id, false, user);
		VariableHistory variableDeserialized = getHistoricVariableInstanceImpl(id, true, user);

		if (deserializeValue) {
			variableDeserialized.setValueSerialized(variableSerialized.getValue());
			variableDeserialized.setValueDeserialized(variableDeserialized.getValue());
			return variableDeserialized;
		}
		else {
			variableSerialized.setValueSerialized(variableSerialized.getValue());
			variableSerialized.setValueDeserialized(variableDeserialized.getValue());
			return variableSerialized;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<VariableHistory> findHistoricVariableInstances(Map<String, Object> filters,
			Optional<Integer> firstResult, Optional<Integer> maxResults, Boolean deserializeValues,
			CIBUser user) throws SystemException {
		// The engine takes the paging and the deserialization flag in the query string, the rest
		// of the query in the body, which is how the sdk splits them too
		Map<String, Object> params = new HashMap<>();
		firstResult.ifPresent(value -> params.put("firstResult", value));
		maxResults.ifPresent(value -> params.put("maxResults", value));
		if (deserializeValues != null) {
			params.put("deserializeValues", deserializeValues);
		}
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/history/variable-instance", params);
		return Arrays.asList(((ResponseEntity<VariableHistory[]>) doPost(url, filters, VariableHistory[].class, user)).getBody());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Integer findHistoricVariableInstancesCount(Map<String, Object> filters, CIBUser user) throws SystemException {
		String url = getEngineRestUrl(user) + "/history/variable-instance/count";
		JsonNode body = ((ResponseEntity<JsonNode>) doPost(url, filters, JsonNode.class, user)).getBody();
		if (body == null) {
			throw new SystemException("No count returned for the historic variable instance query");
		}
		return body.get("count").asInt();
	}

}
