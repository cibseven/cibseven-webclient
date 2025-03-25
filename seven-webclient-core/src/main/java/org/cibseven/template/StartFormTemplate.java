package org.cibseven.template;

import java.util.Map;

import org.cibseven.rest.model.Variable;

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
