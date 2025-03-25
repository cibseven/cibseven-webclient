package org.cibseven.webapp.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariableHistory extends Variable {
	String id;
	String name;
	String processDefinitionKey;
	String processDefinitionId;
	String processInstanceId;
	String executionId;
	String activityInstanceId;
	String caseDefinitionKey;
	String caseDefinitionId;
	String caseInstanceId;
	String caseExecutionId;
	String taskId;
	String errorMessage;
	String tenantId;
	String state;
	String createTime;
	String removalTime;
	String rootProcessInstanceId;
}
