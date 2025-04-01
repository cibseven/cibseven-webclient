package org.cibseven.webapp.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true)
public class CandidateGroupTaskCount {
	private String groupName;
    private int taskCount;
}
