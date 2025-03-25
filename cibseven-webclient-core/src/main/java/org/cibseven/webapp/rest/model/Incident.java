package org.cibseven.webapp.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true) 
public class Incident {
	private String id;
	private String processDefinitionId;
	private String processInstanceId;
	private String executionId;
	private String incidentTimestamp;
	private String incidentType;
	private String activityId;
	private String causeIncidentId;
	private String rootCauseIncidentId;
	private String configuration;
	private String tenantId;
	private String incidentMessage;
	private String jobDefinitionId;
	private String failedActivityId;
}
