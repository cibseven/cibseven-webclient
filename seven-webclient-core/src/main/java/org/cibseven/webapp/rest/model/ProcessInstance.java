package org.cibseven.webapp.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessInstance {
	String businessKey;
	String caseInstanceId;
	String deleteReason;
	Long durationInMillis;
	String endTime;
	String id;
	String processDefinitionId;
	String processDefinitionKey;
	String processDefinitionName;
	String processDefinitionVersion;
	String removalTime;
	String rootProcessInstanceId;
	String startActivityId;
	String startTime;
	String startUserId;
	String state;
	String superCaseInstanceId;
	String superProcessInstanceId;
	String tenantId;
	List<Incident> incidents;
}
