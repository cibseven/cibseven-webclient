package org.cibseven.webapp.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true) 
public class JobDefinition {
	private String id;
	private String processDefinitionId;
	private String processDefinitionKey;
	private String activityId;
	private String jobType;
	private String jobConfiguration;
	private Boolean suspended;
	private Integer overridingJobPriority;
	private String tenantId;
	private String deploymentId;
}
