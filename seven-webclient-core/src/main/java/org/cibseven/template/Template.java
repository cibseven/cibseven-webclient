package org.cibseven.template;

import java.util.List;
import java.util.Map;

import org.cibseven.rest.model.ActivityInstance;
import org.cibseven.rest.model.ActivityInstanceHistory;
import org.cibseven.rest.model.ProcessDiagram;
import org.cibseven.rest.model.Task;
import org.cibseven.rest.model.Variable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

@Data
public class Template {
	private Map<String, Variable> variables;
	private ProcessDiagram bpmDiagram;
	private ActivityInstance activityInstances;
	private List<ActivityInstanceHistory> activityInstanceHistory;
	private Task task;

	public String asJson() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(this);
	}			
}
