package org.cibseven.webapp.template;

import java.util.List;
import java.util.Map;

import org.cibseven.webapp.rest.model.ActivityInstance;
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.cibseven.webapp.rest.model.ProcessDiagram;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.Variable;

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
