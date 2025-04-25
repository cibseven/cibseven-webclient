package org.cibseven.webapp.rest.model;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true)
public class Deployment {
	private Collection<Object> links;
	private String id;
	private String name;
	private String source;
	private String deploymentTime;
	private String tenantId;
	private Map<String, Process> deployedProcessDefinitions;
	private String deployedCaseDefinitions;
	private Object deployedDecisionDefinitions;
	private String deployedDecisionRequirementsDefinitions;
}
