package org.cibseven.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true)
public class ActivityInstanceHistory {
	private String activityId;
	private String activityName;
	private String activityType;
	private String assignee;
	private String calledCaseInstanceId;
	private String calledProcessInstanceId;
	private boolean canceled;
	private boolean completeScope;
	private long durationInMillis;
	private String endTime;
	private String executionId;
	private String id;
	private String parentActivityInstanceId;
	private String processDefinitionId;
	private String processDefinitionKey;
	private String processInstanceId;
	private String removalTime;
	private String rootProcessInstanceId;
	private String startTime;
	private String taskId;
	private String tenantId;
}
