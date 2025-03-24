package org.cibseven.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class Filter {
	private String id;
	private String resourceType;
	private String name;
	private String owner;
	private FilterCriterias query;
	private FilterProperties properties;
	
	public String json() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(this);
	}
	
}
