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

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.utils.URLUtils;
import org.cibseven.webapp.rest.model.Incident;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class IncidentProvider extends SevenProviderBase implements IIncidentProvider {
	
	@Override
	public Long countIncident(Map<String, Object> params, CIBUser user) {
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl() + "/incident/count", params);
		JsonNode response = ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, true)).getBody();
		return response != null ? response.get("count").asLong() : 0L;
	}

	@Override
	public Long countHistoricIncident(Map<String, Object> params, CIBUser user) {
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl() + "/history/incident/count", params);
		JsonNode response = ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, true)).getBody();
		return response != null ? response.get("count").asLong() : 0L;
	}

	@Override
	public Collection<Incident> findIncident(Map<String, Object> params, CIBUser user) {
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl() + "/incident", params);
		Incident[] response = ((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, true)).getBody();
		List<Incident> incidents = response != null ? Arrays.asList(response) : Arrays.asList();
		
		// Enrich incidents with root cause incident data
		for (Incident incident : incidents) {
			if (incident.getId() != null && incident.getRootCauseIncidentId() != null 
					&& !incident.getId().equals(incident.getRootCauseIncidentId())) {
				try {
					// Fetch the root cause incident
					Incident rootCauseIncident = fetchIncidentById(incident.getRootCauseIncidentId(), user);
					if (rootCauseIncident != null) {
						// Map root cause incident data to the specific fields
						incident.setCauseIncidentProcessInstanceId(rootCauseIncident.getProcessInstanceId());
						incident.setCauseIncidentProcessDefinitionId(rootCauseIncident.getProcessDefinitionId());
						incident.setCauseIncidentActivityId(rootCauseIncident.getActivityId());
						incident.setCauseIncidentFailedActivityId(rootCauseIncident.getFailedActivityId());
						incident.setRootCauseIncidentProcessInstanceId(rootCauseIncident.getProcessInstanceId());
						incident.setRootCauseIncidentProcessDefinitionId(rootCauseIncident.getProcessDefinitionId());
						incident.setRootCauseIncidentActivityId(rootCauseIncident.getActivityId());
						incident.setRootCauseIncidentFailedActivityId(rootCauseIncident.getFailedActivityId());
						incident.setRootCauseIncidentConfiguration(rootCauseIncident.getConfiguration());
						incident.setRootCauseIncidentMessage(rootCauseIncident.getIncidentMessage());
					}
				} catch (Exception e) {
					log.warn("Failed to enrich incident with ID: {} and root cause ID: {}", 
						incident.getId(), 
						incident.getRootCauseIncidentId(), 
						e);
				}
			}
		}
		
		return incidents;
	}
	
	private Incident fetchIncidentById(String incidentId, CIBUser user) {
		String url = getEngineRestUrl() + "/incident/" + incidentId;
		ResponseEntity<Incident> response = doGet(url, Incident.class, user, false);
		return response != null ? response.getBody() : null;
	}
	
	@Override
	public List<Incident> findIncidentByInstanceId(String processInstanceId, CIBUser user) {
		String url = getEngineRestUrl() + "/incident?processInstanceId=" + processInstanceId;
		Incident[] response = ((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, false)).getBody();
		return response != null ? Arrays.asList(response) : Arrays.asList();
	}

	@Override
	public Collection<Incident> fetchIncidents(String processDefinitionKey, CIBUser user) {
		String url = getEngineRestUrl() + "/incident?processDefinitionKeyIn=" + processDefinitionKey;
		Incident[] response = ((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, false)).getBody();
		return response != null ? Arrays.asList(response) : Arrays.asList();
	}

	@Override
	public void setIncidentAnnotation(String incidentId, Map<String, Object> data, CIBUser user) {
		String url = getEngineRestUrl() + "/incident/" + incidentId + "/annotation";
		doPut(url, data, user);
	}	

	@Override
	public void retryExternalTask(String externalTaskId, Map<String, Object> data, CIBUser user) {
		String url = getEngineRestUrl() + "/external-task/" + externalTaskId + "/retries";
		doPut(url, data, user);
	}
	
	@Override
	public String findExternalTaskErrorDetails(String externalTaskId, CIBUser user) {
		String url = getEngineRestUrl() + "/external-task/" + externalTaskId + "/errorDetails";
		return doGetWithHeader(url, String.class, user, false, MediaType.ALL).getBody();
	}
	
	@Override
	public String findHistoricExternalTaskErrorDetails(String externalTaskId, CIBUser user) {
		String url = getEngineRestUrl() + "/history/external-task-log/" + externalTaskId + "/error-details";
		return doGetWithHeader(url, String.class, user, false, MediaType.ALL).getBody();
	}
	
	@Override
	public Collection<Incident> findHistoricIncidents(Map<String, Object> params, CIBUser user) {
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl() + "/history/incident", params);
		Incident[] response = ((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, true)).getBody();
		List<Incident> incidents = response != null ? Arrays.asList(response) : Arrays.asList();
		
		// Enrich historic incidents with root cause incident data (same enrichment algorithm as current incidents)
		for (Incident incident : incidents) {
			if (incident.getId() != null && incident.getRootCauseIncidentId() != null 
					&& !incident.getId().equals(incident.getRootCauseIncidentId())) {
				try {
					// For historic incidents, try to fetch the root cause from historic incidents first, then from current incidents
					Incident rootCauseIncident = fetchHistoricIncidentById(incident.getRootCauseIncidentId(), user);
					if (rootCauseIncident != null) {
						// Map root cause incident data to the specific fields
						incident.setCauseIncidentProcessInstanceId(rootCauseIncident.getProcessInstanceId());
						incident.setCauseIncidentProcessDefinitionId(rootCauseIncident.getProcessDefinitionId());
						incident.setCauseIncidentActivityId(rootCauseIncident.getActivityId());
						incident.setCauseIncidentFailedActivityId(rootCauseIncident.getFailedActivityId());
						incident.setRootCauseIncidentProcessInstanceId(rootCauseIncident.getProcessInstanceId());
						incident.setRootCauseIncidentProcessDefinitionId(rootCauseIncident.getProcessDefinitionId());
						incident.setRootCauseIncidentActivityId(rootCauseIncident.getActivityId());
						incident.setRootCauseIncidentFailedActivityId(rootCauseIncident.getFailedActivityId());
						incident.setRootCauseIncidentConfiguration(rootCauseIncident.getConfiguration());
						incident.setRootCauseIncidentMessage(rootCauseIncident.getIncidentMessage());
					}
				} catch (Exception e) {
					log.warn("Failed to enrich historic incident with ID: {} and root cause ID: {}", 
						incident.getId(), 
						incident.getRootCauseIncidentId(), 
						e);
				}
			}
		}
		
		return incidents;
	}
	
	private Incident fetchHistoricIncidentById(String incidentId, CIBUser user) {
		try {
			Map<String, Object> params = Map.of("incidentId", incidentId);
			String url = URLUtils.buildUrlWithParams(getEngineRestUrl() + "/history/incident", params);
			Incident[] response = ((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, true)).getBody();
			// Return the first incident if found, null otherwise
			return (response != null && response.length > 0) ? response[0] : null;
		} catch (Exception e) {
			// Historic incident not found, return null
			return null;
		}
	}
	
	@Override
	public String findHistoricStacktraceByJobId(String jobId, CIBUser user) {
		String url = getEngineRestUrl() + "/history/job-log/" + jobId + "/stacktrace";
		return doGetWithHeader(url, String.class, user, false, MediaType.ALL).getBody();
	}
	
	@Override
	public Collection<Incident> fetchIncidentsByInstanceAndActivityId(String processDefinitionId, String activityId, CIBUser user) {
	    String url = getEngineRestUrl() + "/incident?processDefinitionId=" + processDefinitionId + "&activityId=" + activityId;
	    Incident[] response = ((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, false)).getBody();
	    return response != null ? Arrays.asList(response) : Arrays.asList();
	}
	
}
