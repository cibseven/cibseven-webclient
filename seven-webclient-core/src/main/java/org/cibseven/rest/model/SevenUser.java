package org.cibseven.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true) 
public class SevenUser {
	private String id;
	private String firstName;
	private String lastName;
	private String email;
}
