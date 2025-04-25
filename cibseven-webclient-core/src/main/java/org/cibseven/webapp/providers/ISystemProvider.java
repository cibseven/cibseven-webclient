package org.cibseven.webapp.providers;

import java.util.Collection;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Metric;

import com.fasterxml.jackson.databind.JsonNode;

public interface ISystemProvider {
	
	public JsonNode getTelemetryData(CIBUser user);
	public Collection<Metric> getMetrics(Map<String, Object> queryParams, CIBUser user);
}
