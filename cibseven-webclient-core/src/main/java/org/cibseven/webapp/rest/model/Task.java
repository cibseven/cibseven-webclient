package org.cibseven.webapp.rest.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {
	String assignee;
	String caseDefinitionId;
	String caseExecutionId;
	String caseInstanceId;
	String delegationState;
	String description;
	String executionId;
	String formKey;
	String id;
	String name;
	String owner;
	String parentTaskId;
	long priority;
	String suspended;
	String tenantId;
	CamundaForm camundaFormRef;
	
	@JsonProperty("created") @JsonAlias({"creationDate"}) String created;
	@JsonProperty("due") @JsonAlias({"dueDate"}) String due;
	@JsonProperty("followUp") @JsonAlias({"followUpDate"}) String followUp;
	@JsonProperty("taskDefinitionKey") @JsonAlias({"taskDefinitionId"}) String taskDefinitionKey;
	@JsonProperty("processDefinitionId") @JsonAlias({"processDefinitionKey"}) String processDefinitionId;
	@JsonProperty("processInstanceId") @JsonAlias({"processInstanceKey"}) String processInstanceId;
	
	public String json() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(this);
	}
}
