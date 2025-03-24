package org.cibseven.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class UserGroup {
	private String id;
	private String name;
	private String type;
	
	public String json() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(this);
	}
}
