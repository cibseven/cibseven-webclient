package org.cibseven.webapp.rest.model;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor 
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessStart {
	private Collection<Object> links;
	private String id;
	private String definitionId;
	private String businessKey;
	private String caseInstanceId;
	private Boolean ended;
	private Boolean suspended;
	private String tenantId;
}
