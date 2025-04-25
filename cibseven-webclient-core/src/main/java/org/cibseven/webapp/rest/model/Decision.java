package org.cibseven.webapp.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Decision {
	private String id;
	private String key;
	private String category;
	private String name;
	private int version;
	private String resource;
	private String deploymentId;
	private String decisionRequirementsDefinitionId;
	private String decisionRequirementsDefinitionKey;
	private String tenantId;
	private String versionTag;
	private Integer historyTimeToLive;
	long allInstances;
}
