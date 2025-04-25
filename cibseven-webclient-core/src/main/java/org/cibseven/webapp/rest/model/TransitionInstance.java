package org.cibseven.webapp.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true)
public class TransitionInstance {
	private String id;
	private String parentActivityInstanceId;
	private String processInstanceId;
	private String processDefinitionId;
	private String activityId;
	private String activityName;
	private String activityType;
	private String executionId;
	private String targetActivityId;
	private List<String> incidentIds;
}
