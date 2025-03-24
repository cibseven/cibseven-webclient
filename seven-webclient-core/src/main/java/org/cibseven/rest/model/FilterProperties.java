package org.cibseven.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class FilterProperties {
	private String color;
	private boolean showUndefinedVariable;
	private String description;
	private boolean refresh;
	private int priority;
}
