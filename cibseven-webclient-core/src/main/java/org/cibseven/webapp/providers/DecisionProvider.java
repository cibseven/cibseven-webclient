package org.cibseven.webapp.providers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Decision;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class DecisionProvider extends SevenProviderBase implements IDecisionProvider{
	
	@Override
	public Collection<Decision> getDecisionDefinitionList(Map<String, Object> queryParams) {
		String url = buildUrlWithParams(camundaUrl + "/engine-rest/decision-definition", queryParams);
		return Arrays.asList(((ResponseEntity<Decision[]>) doGet(url, Decision[].class, null, false)).getBody());
	}

	@Override
	public Object getDecisionDefinitionListCount(Map<String, Object> queryParams) {
		String url = buildUrlWithParams(camundaUrl + "/engine-rest/decision-definition/count", queryParams);    
		return ((ResponseEntity<Object>) doGet(url, Object.class, null, false)).getBody();
	}
	
	@Override
	public Decision getDecisionDefinitionByKey(String key) {
		String url = camundaUrl + "/engine-rest/decision-definition/key/" + key;
		return ((ResponseEntity<Decision>) doGet(url, Decision.class, null, false)).getBody();
	}

	@Override
	public Object getDiagramByKey(String key) {
		String url = camundaUrl + "/engine-rest/decision-definition/key/" + key + "/diagram";
		return ((ResponseEntity<Object>) doGet(url, Object.class, null, false)).getBody();
	}

	@Override
	public Object evaluateDecisionDefinitionByKey(Map<String, Object> data, String key, CIBUser user) {
		String url = camundaUrl + "/engine-rest/decision-definition/key/" + key + "/evaluate";
		return ((ResponseEntity<Object>) doPost(url, data, null, user)).getBody();
	}

	@Override
	public void updateHistoryTTLByKey(Map<String, Object> data, String key, CIBUser user) {
		String url = camundaUrl + "/engine-rest/decision-definition/key/" + key + "/history-time-to-live";
		doPut(url, data, user);
	}

	@Override
	public Decision getDecisionDefinitionByKeyAndTenant(String key, String tenant) {
		String url = camundaUrl + "/engine-rest/decision-definition/key/" + key + "/tenant-id/" + tenant;
		return ((ResponseEntity<Decision>) doGet(url, Decision.class, null, false)).getBody();
	}

	@Override
	public Object getDiagramByKeyAndTenant(String key, String tenant) {
		String url = camundaUrl + "/engine-rest/decision-definition/key/" + key + "/tenant-id/" + tenant + "/diagram";
		return ((ResponseEntity<Decision>) doGet(url, Decision.class, null, false)).getBody();
	}

	@Override
	public Object evaluateDecisionDefinitionByKeyAndTenant(String key, String tenant) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object updateHistoryTTLByKeyAndTenant(String key, String tenant) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getXmlByKey(String key) {
		String url = camundaUrl + "/engine-rest/decision-definition/key/" + key + "/xml";
		return ((ResponseEntity<Decision>) doGet(url, Decision.class, null, false)).getBody();
	}

	@Override
	public Object getXmlByKeyAndTenant(String key, String tenant) {
		String url = camundaUrl + "/engine-rest/decision-definition/key/" + key + "/tenant-id/" + tenant + "/xml";
		return ((ResponseEntity<Decision>) doGet(url, Decision.class, null, false)).getBody();
	}

	@Override
	public Decision getDecisionDefinitionById(String id, Optional<Boolean> extraInfo) {
		String url = camundaUrl + "/engine-rest/decision-definition/" + id;
		Decision decision = ((ResponseEntity<Decision>) doGet(url, Decision.class, null, false)).getBody();
		if (extraInfo.isPresent() && extraInfo.get()) {
			String urlCount = camundaUrl + "/engine-rest/history/decision-instance/count?decisionDefinitionId=" + decision.getId();
			decision.setAllInstances(((ResponseEntity<JsonNode>) doGet(urlCount, JsonNode.class, null, false)).getBody().get("count").asLong());
		}
		return decision;
	}

	@Override
	public Object getDiagramById(String id) {
		String url = camundaUrl + "/engine-rest/decision-definition/" + id + "/diagram";
		return ((ResponseEntity<Decision>) doGet(url, Decision.class, null, false)).getBody();
	}

	@Override
	public Object evaluateDecisionDefinitionById(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object updateHistoryTTLById(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getXmlById(String id) {
		String url = camundaUrl + "/engine-rest/decision-definition/" + id + "/xml";
		return ((ResponseEntity<Object>) doGet(url, Object.class, null, false)).getBody();
	}
	
	@Override
	public Collection<Decision> getDecisionVersionsByKey(String key, Optional<Boolean> lazyLoad) {
		String url = camundaUrl + "/engine-rest/decision-definition?key=" + key + "&sortBy=version&sortOrder=desc";
		Collection<Decision> decisions = Arrays.asList(((ResponseEntity<Decision[]>) doGet(url, Decision[].class, null, false)).getBody());		
		
		if (!lazyLoad.isPresent() || (lazyLoad.isPresent() && !lazyLoad.get())) {
			for(Decision decision : decisions) {
				String urlCount = camundaUrl + "/engine-rest/history/decision-instance/count?decisionDefinitionId=" + decision.getId();
				decision.setAllInstances(((ResponseEntity<JsonNode>) doGet(urlCount, JsonNode.class, null, false)).getBody().get("count").asLong());
			}
		}
		return decisions;
	}
	
	@Override
	public Object getHistoricDecisionInstances(Map<String, Object> queryParams){
		String url = buildUrlWithParams(camundaUrl + "/engine-rest/history/decision-instance", queryParams);
		return ((ResponseEntity<Object>) doGet(url, Object.class, null, false)).getBody();
	}
	
	@Override
	public Object getHistoricDecisionInstanceCount(Map<String, Object> queryParams){
		String url = buildUrlWithParams(camundaUrl + "/engine-rest/history/decision-instance/count", queryParams);
		return ((ResponseEntity<Object>) doGet(url, Object.class, null, false)).getBody();
	}
	
	@Override
	public Object getHistoricDecisionInstanceById(String id, Map<String, Object> queryParams){
		String url = buildUrlWithParams(camundaUrl + "/engine-rest/history/decision-instance/" + id, queryParams);
		return ((ResponseEntity<Object>) doGet(url, Object.class, null, false)).getBody();
	}
	
	@Override
	public Object deleteHistoricDecisionInstances(Map<String, Object> body){
		String url = camundaUrl + "/engine-rest/history/decision-instance/delete";
		return ((ResponseEntity<Object>) doPost(url, body, null, null)).getBody();
	}
	
	@Override
	public Object setHistoricDecisionInstanceRemovalTime(Map<String, Object> body){
		String url = camundaUrl + "/engine-rest/history/decision-instance/set-removal-time";
		return ((ResponseEntity<Object>) doPost(url, body, null, null)).getBody();
	}
	
	@Override
	protected HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user) {
		if (user != null) headers.add("Authorization", user.getAuthToken());
		return headers;
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