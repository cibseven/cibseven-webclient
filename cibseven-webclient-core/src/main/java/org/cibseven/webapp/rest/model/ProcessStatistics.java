package org.cibseven.webapp.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true) 
public class ProcessStatistics {
	private String id;
	private long instances;
	private long failedJobs;
	private List<IncidentInfo> incidents;
	ProcessDefinitionInfo definition;
}
