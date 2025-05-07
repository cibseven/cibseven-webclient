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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Incident;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class IncidentProvider extends SevenProviderBase implements IIncidentProvider {
	
	@Override
	public Long countIncident(
			Optional<String> incidentId, Optional<String> incidentType, Optional<String> incidentMessage, Optional<String> processDefinitionId, Optional<String> processDefinitionKeyIn,
			Optional<String> processInstanceId, Optional<String> executionId, Optional<String> activityId, Optional<String> causeIncidentId, Optional<String> rootCauseIncidentId, 
			Optional<String> configuration, Optional<String> tenantIdIn, Optional<String> jobDefinitionIdIn, Optional<String> name, CIBUser user) {
		
		String url = cibsevenUrl + "/engine-rest/incident/count";
		
		String param = "";
		param += addQueryParameter(param, "incidentId", incidentId, true);
		param += addQueryParameter(param, "incidentType", incidentType, true);
		param += addQueryParameter(param, "incidentMessage", incidentMessage, true);
		param += addQueryParameter(param, "processDefinitionId", processDefinitionId, false);
		param += addQueryParameter(param, "processDefinitionKeyIn", processDefinitionKeyIn, false);
		param += addQueryParameter(param, "processInstanceId", processInstanceId, true);
		param += addQueryParameter(param, "executionId", executionId, true);
		param += addQueryParameter(param, "activityId", activityId, true);
		param += addQueryParameter(param, "causeIncidentId", causeIncidentId, true);
		param += addQueryParameter(param, "rootCauseIncidentId", rootCauseIncidentId, true);
		param += addQueryParameter(param, "configuration", configuration, true);
		param += addQueryParameter(param, "tenantIdIn", tenantIdIn, true);
		param += addQueryParameter(param, "jobDefinitionIdIn", jobDefinitionIdIn, true);
		param += addQueryParameter(param, "name", name, true);
		
		url += param;
		
		return ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, false)).getBody().get("count").asLong();
	}

	@Override
	public Collection<Incident> findIncident(
			Optional<String> incidentId, Optional<String> incidentType, Optional<String> incidentMessage, Optional<String> processDefinitionId, Optional<String> processDefinitionKeyIn, 
			Optional<String> processInstanceId, Optional<String> executionId, Optional<String> activityId, Optional<String> causeIncidentId, Optional<String> rootCauseIncidentId, 
			Optional<String> configuration, Optional<String> tenantIdIn, Optional<String> jobDefinitionIdIn, CIBUser user) {
		
		String url = cibsevenUrl + "/engine-rest/incident";
		
		String param = "";
		param += addQueryParameter(param, "incidentId", incidentId, true);
		param += addQueryParameter(param, "incidentType", incidentType, true);
		param += addQueryParameter(param, "incidentMessage", incidentMessage, true);
		param += addQueryParameter(param, "processDefinitionId", processDefinitionId, false);
		param += addQueryParameter(param, "processDefinitionKeyIn", processDefinitionKeyIn, false);
		param += addQueryParameter(param, "processInstanceId", processInstanceId, true);
		param += addQueryParameter(param, "executionId", executionId, true);
		param += addQueryParameter(param, "activityId", activityId, true);
		param += addQueryParameter(param, "causeIncidentId", causeIncidentId, true);
		param += addQueryParameter(param, "rootCauseIncidentId", rootCauseIncidentId, true);
		param += addQueryParameter(param, "configuration", configuration, true);
		param += addQueryParameter(param, "tenantIdIn", tenantIdIn, true);
		param += addQueryParameter(param, "jobDefinitionIdIn", jobDefinitionIdIn, true);
		
		url += param;
		
		return Arrays.asList(((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, false)).getBody());
	}
	
	@Override
	public List<Incident> findIncidentByInstanceId(String processInstanceId, CIBUser user) {
		String url = cibsevenUrl + "/engine-rest/incident";
		
		String param = "";
		param += addQueryParameter(param, "processInstanceId", Optional.of(processInstanceId), true);
		
		url += param;
		
		return Arrays.asList(((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, false)).getBody());
	}

	@Override
	public Collection<Incident> fetchIncidents(String processDefinitionKey, CIBUser user) {
		String url = cibsevenUrl + "/engine-rest/incident?processDefinitionKeyIn=" + processDefinitionKey;
		return Arrays.asList(((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, false)).getBody());
	}	

	@Override
	public void setIncidentAnnotation(String incidentId, Map<String, Object> data, CIBUser user) {
		String url = cibsevenUrl + "/engine-rest/incident/" + incidentId + "/annotation";
		doPut(url, data, user);
	}	
	
	@Override
	public Collection<Incident> fetchIncidentsByInstanceAndActivityId(String processDefinitionId, String activityId, CIBUser user) {
	    String url = cibsevenUrl + "/engine-rest/incident?processDefinitionId=" + processDefinitionId + "&activityId=" + activityId;
	    return Arrays.asList(((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, false)).getBody());
	}	
	
}
