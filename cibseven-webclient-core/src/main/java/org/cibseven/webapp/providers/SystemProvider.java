package org.cibseven.webapp.providers;

import org.cibseven.webapp.auth.CIBUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SystemProvider extends SevenProviderBase implements ISystemProvider {


	@Override
	protected HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user) {
		if (user != null) headers.add("Authorization", user.getAuthToken());
		return headers;
	}

	@Override
	public JsonNode getTelemetryData(CIBUser user) {
		String url = camundaUrl + "/engine-rest/telemetry/data";
		return ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, false)).getBody();
	}	
	
}
