package org.cibseven.webapp.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true) 
public class SevenTenant {
	private String id;
	private String name;

	public String json() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(this);
	}
}
