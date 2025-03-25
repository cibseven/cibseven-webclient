package org.cibseven.webapp.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor 
@JsonIgnoreProperties(ignoreUnknown = true)
public class Process {
	String category;
	String deploymentId;
	String description;
	String diagram;
	String historyTimeToLive;
	String id;
	String key;
	String name;
	String resource;
	Boolean startableInTasklist;
	String suspended;
	String tenantId;
	String version;
	String versionTag;
	List<String> calledFromActivityIds;
	long runningInstances;
	long allInstances;
	long completedInstances;
	long incidents;
}
