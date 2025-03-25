package org.cibseven.webapp.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data @AllArgsConstructor @NoArgsConstructor @ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
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
