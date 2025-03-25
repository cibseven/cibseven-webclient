package org.cibseven.webapp.template;

import java.util.Map;

import org.cibseven.webapp.rest.model.Variable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

@Data
public class StartFormTemplate {
	private Map<String, Variable> variables;
	
	public String asJson() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(this);
	}
}
