package org.cibseven.webapp.rest.model;

import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessInstance {
	
	private String id;
    private String definitionId;
    private String businessKey;
    private String caseInstanceId;
    private Boolean suspended;
    private String tenantId;
    private Collection<Object> links;
}
