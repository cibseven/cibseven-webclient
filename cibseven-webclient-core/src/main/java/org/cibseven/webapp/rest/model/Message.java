package org.cibseven.webapp.rest.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true) 
public class Message {
	private String resultType;
	private Map<String, Object> execution;
	private Map<String, Object> processInstance;
	private Map<String, Object> variables;
}
