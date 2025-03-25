package org.cibseven.webapp.providers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Incident;
import org.springframework.http.HttpHeaders;
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
		
		String url = camundaUrl + "/engine-rest/incident/count";
		
		String param = "";
		param += addQueryParameter(param, "incidentId", incidentId);
		param += addQueryParameter(param, "incidentType", incidentType);
		param += addQueryParameter(param, "incidentMessage", incidentMessage);
		param += addQueryParameter(param, "processDefinitionId", processDefinitionId);
		param += addQueryParameter(param, "processDefinitionKeyIn", processDefinitionKeyIn);
		param += addQueryParameter(param, "processInstanceId", processInstanceId);
		param += addQueryParameter(param, "executionId", executionId);
		param += addQueryParameter(param, "activityId", activityId);
		param += addQueryParameter(param, "causeIncidentId", causeIncidentId);
		param += addQueryParameter(param, "rootCauseIncidentId", rootCauseIncidentId);
		param += addQueryParameter(param, "configuration", configuration);
		param += addQueryParameter(param, "tenantIdIn", tenantIdIn);
		param += addQueryParameter(param, "jobDefinitionIdIn", jobDefinitionIdIn);
		param += addQueryParameter(param, "name", name);
		
		url += param;
		
		return ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, false)).getBody().get("count").asLong();
	}

	@Override
	public Collection<Incident> findIncident(
			Optional<String> incidentId, Optional<String> incidentType, Optional<String> incidentMessage, Optional<String> processDefinitionId, Optional<String> processDefinitionKeyIn, 
			Optional<String> processInstanceId, Optional<String> executionId, Optional<String> activityId, Optional<String> causeIncidentId, Optional<String> rootCauseIncidentId, 
			Optional<String> configuration, Optional<String> tenantIdIn, Optional<String> jobDefinitionIdIn, CIBUser user) {
		
		String url = camundaUrl + "/engine-rest/incident";
		
		String param = "";
		param += addQueryParameter(param, "incidentId", incidentId);
		param += addQueryParameter(param, "incidentType", incidentType);
		param += addQueryParameter(param, "incidentMessage", incidentMessage);
		param += addQueryParameter(param, "processDefinitionId", processDefinitionId);
		param += addQueryParameter(param, "processDefinitionKeyIn", processDefinitionKeyIn);
		param += addQueryParameter(param, "processInstanceId", processInstanceId);
		param += addQueryParameter(param, "executionId", executionId);
		param += addQueryParameter(param, "activityId", activityId);
		param += addQueryParameter(param, "causeIncidentId", causeIncidentId);
		param += addQueryParameter(param, "rootCauseIncidentId", rootCauseIncidentId);
		param += addQueryParameter(param, "configuration", configuration);
		param += addQueryParameter(param, "tenantIdIn", tenantIdIn);
		param += addQueryParameter(param, "jobDefinitionIdIn", jobDefinitionIdIn);
		
		url += param;
		
		return Arrays.asList(((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, false)).getBody());
	}
	
	@Override
	public List<Incident> findIncidentByInstanceId(String processInstanceId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/incident";
		
		String param = "";
		param += addQueryParameter(param, "processInstanceId", Optional.of(processInstanceId));
		
		url += param;
		
		return Arrays.asList(((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, false)).getBody());
	}

	@Override
	public Collection<Incident> fetchIncidents(String processDefinitionKey, CIBUser user) {
		String url = camundaUrl + "/engine-rest/incident?processDefinitionKeyIn=" + processDefinitionKey;
		return Arrays.asList(((ResponseEntity<Incident[]>) doGet(url, Incident[].class, user, false)).getBody());
	}	
	
	@Override
	protected HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user) {
		if (user != null) headers.add("Authorization", user.getAuthToken());
		return headers;
	}	

}
