package org.cibseven.webapp.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true)
public class ActivityInstance {
	private String activityId;
	private String activityName;
	private String activityType;
	private List<String> executionIds;
	private String id;
	private String name;
	private String parentActivityInstanceId;
	private String processDefinitionId;
	private String processInstanceId;
	private List<ActivityInstance> childActivityInstances;
	private List<TransitionInstance> childTransitionInstances;

}
