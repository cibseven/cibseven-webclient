package org.cibseven.webapp.rest.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true) 
public class ProcessDefinitionInfo {
	private String id;
    private String key;
    private String category;
    private String description;
    private String name;
    private Integer version;
    private String resource;
    private String deploymentId;
    private String diagram;
    private Boolean suspended;
    private String tenantId;
    private String versionTag;
    private Integer historyTimeToLive;
    private Boolean startableInTasklist;
}
