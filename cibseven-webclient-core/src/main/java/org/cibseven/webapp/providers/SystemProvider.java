package org.cibseven.webapp.providers;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Metric;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SystemProvider extends SevenProviderBase implements ISystemProvider {

	@Override
	public JsonNode getTelemetryData(CIBUser user) {
		String url = camundaUrl + "/engine-rest/telemetry/data";
		return ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, false)).getBody();
	}

	@Override
	public Collection<Metric> getMetrics(Map<String, Object> queryParams, CIBUser user) {
		// TODO Auto-generated method stub
		return Collections.emptyList();
	}	
	
}
