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
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Decision;
import org.cibseven.webapp.rest.model.HistoricDecisionInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class DecisionProvider extends SevenProviderBase implements IDecisionProvider{
	
	@Override
	public Collection<Decision> getDecisionDefinitionList(Map<String, Object> queryParams, CIBUser user) {
		String url = buildUrlWithParams(getEngineRestUrl() + "/decision-definition", queryParams);
		return Arrays.asList(((ResponseEntity<Decision[]>) doGet(url, Decision[].class, user, true)).getBody());
	}

	@Override
	public Long getDecisionDefinitionListCount(Map<String, Object> queryParams, CIBUser user) {
		String url = buildUrlWithParams(getEngineRestUrl() + "/decision-definition/count", queryParams);    
		JsonNode response = ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, true)).getBody();
		return response != null ? response.get("count").asLong() : 0L;
	}
	
	@Override
	public Decision getDecisionDefinitionByKey(String key, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition/key/" + key;
		return ((ResponseEntity<Decision>) doGet(url, Decision.class, user, false)).getBody();
	}

	@Override
	public Object getDiagramByKey(String key, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition/key/" + key + "/diagram";
		return ((ResponseEntity<Object>) doGet(url, Object.class, user, false)).getBody();
	}

	@Override
	public Object evaluateDecisionDefinitionByKey(Map<String, Object> data, String key, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition/key/" + key + "/evaluate";
		return ((ResponseEntity<Object>) doPost(url, data, null, user)).getBody();
	}

	@Override
	public void updateHistoryTTLByKey(Map<String, Object> data, String key, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition/key/" + key + "/history-time-to-live";
		doPut(url, data, user);
	}

	@Override
	public Decision getDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition/key/" + key + "/tenant-id/" + tenant;
		return ((ResponseEntity<Decision>) doGet(url, Decision.class, user, false)).getBody();
	}

	@Override
	public Object getDiagramByKeyAndTenant(String key, String tenant, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition/key/" + key + "/tenant-id/" + tenant + "/diagram";
		return ((ResponseEntity<Decision>) doGet(url, Decision.class, user, false)).getBody();
	}

	@Override
	public Object evaluateDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object updateHistoryTTLByKeyAndTenant(String key, String tenant, CIBUser user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getXmlByKey(String key, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition/key/" + key + "/xml";
		return ((ResponseEntity<Decision>) doGet(url, Decision.class, user, false)).getBody();
	}

	@Override
	public Object getXmlByKeyAndTenant(String key, String tenant, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition/key/" + key + "/tenant-id/" + tenant + "/xml";
		return ((ResponseEntity<Decision>) doGet(url, Decision.class, user, false)).getBody();
	}

	@Override
	public Decision getDecisionDefinitionById(String id, Optional<Boolean> extraInfo, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition/" + id;
		Decision decision = ((ResponseEntity<Decision>) doGet(url, Decision.class, user, false)).getBody();
		if (decision != null && extraInfo.isPresent() && extraInfo.get()) {
			String urlCount = getEngineRestUrl() + "/history/decision-instance/count?decisionDefinitionId=" + decision.getId();
			JsonNode body = ((ResponseEntity<JsonNode>) doGet(urlCount, JsonNode.class, user, false)).getBody();
			if (body == null)
				throw new NullPointerException();
			decision.setAllInstances(body.get("count").asLong());
		}
		return decision;
	}

	@Override
	public Object getDiagramById(String id, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition/" + id + "/diagram";
		return ((ResponseEntity<Decision>) doGet(url, Decision.class, user, false)).getBody();
	}

	@Override
	public Object evaluateDecisionDefinitionById(String id, CIBUser user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateHistoryTTLById(String id, Map<String, Object> data, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition/" + id + "/history-time-to-live";
		doPut(url, data, user);
	}

	@Override
	public Object getXmlById(String id, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition/" + id + "/xml";
		return ((ResponseEntity<Object>) doGet(url, Object.class, user, false)).getBody();
	}
	
	@Override
	public Collection<Decision> getDecisionVersionsByKey(String key, Optional<Boolean> lazyLoad, CIBUser user) {
		String url = getEngineRestUrl() + "/decision-definition?key=" + key + "&sortBy=version&sortOrder=desc";
		Collection<Decision> decisions = Arrays.asList(((ResponseEntity<Decision[]>) doGet(url, Decision[].class, user, false)).getBody());		
		
		if (!lazyLoad.isPresent() || (lazyLoad.isPresent() && !lazyLoad.get())) {
			for(Decision decision : decisions) {
				String urlCount = getEngineRestUrl() + "/history/decision-instance/count?decisionDefinitionId=" + decision.getId();
				JsonNode body = ((ResponseEntity<JsonNode>) doGet(urlCount, JsonNode.class, user, false)).getBody();
				if (body == null)
					throw new NullPointerException();

				decision.setAllInstances(body.get("count").asLong());
			}
		}
		return decisions;
	}
	
	@Override
	public Collection<HistoricDecisionInstance> getHistoricDecisionInstances(Map<String, Object> queryParams, CIBUser user) {
		String url = getEngineRestUrl() + "/history/decision-instance" + this.encodeQueryParams(queryParams);
		HistoricDecisionInstance[] arr = ((ResponseEntity<HistoricDecisionInstance[]>) doGet(url, HistoricDecisionInstance[].class, user, true)).getBody();
		return Arrays.asList(arr);
	}
	
	@Override
	public Long getHistoricDecisionInstanceCount(Map<String, Object> queryParams, CIBUser user) {
		String url = getEngineRestUrl() + "/history/decision-instance/count" + this.encodeQueryParams(queryParams);
		JsonNode response = ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, true)).getBody();
		return response != null ? response.get("count").asLong() : 0L;
	}
	
	@Override
	public HistoricDecisionInstance getHistoricDecisionInstanceById(String id, Map<String, Object> queryParams, CIBUser user) {
		String url = buildUrlWithParams(getEngineRestUrl() + "/history/decision-instance/" + id, queryParams);
		return ((ResponseEntity<HistoricDecisionInstance>) doGet(url, HistoricDecisionInstance.class, user, true)).getBody();
	}
	
	@Override
	public Object deleteHistoricDecisionInstances(Map<String, Object> body, CIBUser user) {
		String url = getEngineRestUrl() + "/history/decision-instance/delete";
		return ((ResponseEntity<Object>) doPost(url, body, null, user)).getBody();
	}
	
	@Override
	public Object setHistoricDecisionInstanceRemovalTime(Map<String, Object> body, CIBUser user) {
		String url = getEngineRestUrl() + "/history/decision-instance/set-removal-time";
		return ((ResponseEntity<Object>) doPost(url, body, null, null)).getBody();
	}
	
	private String buildUrlWithParams(String baseUrl, Map<String, Object> queryParams) {
	    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
	    queryParams.forEach((key, value) -> {
	        if (value != null) {
	            builder.queryParam(key, value);
	        }
	    });
	    return builder.toUriString();
	}

	
}