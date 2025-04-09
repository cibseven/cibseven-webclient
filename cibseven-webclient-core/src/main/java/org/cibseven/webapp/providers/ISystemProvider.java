package org.cibseven.webapp.providers;

import org.cibseven.webapp.auth.CIBUser;

import com.fasterxml.jackson.databind.JsonNode;

public interface ISystemProvider {
	
	public JsonNode getTelemetryData(CIBUser user);
	
}
