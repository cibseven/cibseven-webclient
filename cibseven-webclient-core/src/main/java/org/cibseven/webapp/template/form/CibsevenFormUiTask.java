package org.cibseven.webapp.template.form;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.logger.TaskLogger;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.template.StartFormTemplate;
import org.cibseven.webapp.template.Template;
import org.cibseven.webapp.template.TemplateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CibsevenFormUiTask implements TemplateTask {

	@Autowired protected BpmProvider bpmProvider;
	
	public static final String FORM_REFERENCE = "formReference";
	public static final String USER_INSTRUCTION = "userInstruction";
	public static final String FORMULAR_CONTENT = "formularContent";
	public static final String FORM_VARIABLES = "formVariables";
	
	@Override
	public Template getTemplate(String taskId, Optional<String> locale, CIBUser user) throws Exception  {
		Template template = new Template();
		template.setTask(bpmProvider.findTaskById(taskId, user));
		
		log(template.getTask(), "[INFO] Loading form template", getClass().getSimpleName());
		
		List<String> variablesList = new ArrayList<>();
		variablesList.add(FORM_REFERENCE);
		variablesList.add(USER_INSTRUCTION);
		variablesList.add(FORMULAR_CONTENT);
		
		template.setVariables(bpmProvider.fetchFormVariables(variablesList, taskId, user));
		template.setBpmDiagram(bpmProvider.fetchDiagram(template.getTask().getProcessDefinitionId(), user));
		template.setActivityInstances(bpmProvider.findActivityInstances(template.getTask().getProcessInstanceId(), user));
		template.setActivityInstanceHistory(bpmProvider.findActivityInstanceHistory(template.getTask().getProcessInstanceId(), user));

		log(template.getTask(), "[INFO] Fetched variables", getClass().getSimpleName());
		
		Map<String, Variable> onTemplate = template.getVariables();
		Variable onInputData = new Variable();
		Variable onformDataVariables = new Variable();
		
		ResponseEntity <byte[]> formularContent = bpmProvider.getDeployedForm(taskId, user);		
		String formularContentString = new String(formularContent.getBody(), StandardCharsets.UTF_8);
		
		String formDataJsonString = getFormJsonDataVariables(taskId, user);
		
		onInputData.setName(FORMULAR_CONTENT);
		onInputData.setValue(formularContentString);
		onTemplate.put(FORMULAR_CONTENT, onInputData);
		onformDataVariables.setName(FORM_VARIABLES);
		onformDataVariables.setValue(formDataJsonString);
		onTemplate.put(FORM_VARIABLES, onformDataVariables);
		template.setVariables(onTemplate);
		return template;
	}

	private String getFormJsonDataVariables(String taskId, CIBUser user) {
		Map<String, Variable> formVariables = bpmProvider.fetchFormVariables(taskId, false, user);
        LinkedHashMap<String, Object> jsonFormat = new LinkedHashMap<>();      
        
        for (Map.Entry<String, Variable> entry : formVariables.entrySet()) {
            if (entry.getValue() != null) {
                jsonFormat.put(entry.getKey(), entry.getValue().getValue());
            }
        }        
        
        ObjectMapper objectMapper = new ObjectMapper();        
        String formDataJsonString = "";
		try {
        	formDataJsonString  = objectMapper.writeValueAsString(jsonFormat);
        } catch (Exception e) {}
		
        return formDataJsonString;
	}

	@Override
	public StartFormTemplate getStartFormTemplate(String processDefinitionId, Optional<String> processDefinitionKey, Optional<String> locale, CIBUser user) throws Exception {
		log(processDefinitionId, "[INFO] Loading form start event template", getClass().getSimpleName());
		
		StartFormTemplate template = new StartFormTemplate();

		
		Variable onInputData = new Variable();
		
		ResponseEntity <byte[]> formularContent = bpmProvider.getDeployedStartForm(processDefinitionId, user);
		String formularContentString = new String(formularContent.getBody(), StandardCharsets.UTF_8);
		
		Map<String, Variable> onTemplate = new HashMap<>();
		onInputData.setName(FORMULAR_CONTENT);
		onInputData.setValue(formularContentString);
		onTemplate.put(FORMULAR_CONTENT, onInputData);
		
		template.setVariables(onTemplate);

		return template;
	}
	
	protected void log(Task task, String message, String classname) {
		TaskLogger logger = new TaskLogger(task.getProcessDefinitionId(), task.getProcessInstanceId(), task.getName(), task.getId());
		logger.info(message + " for task with name=" + task.getName() + " and ID=" + task.getId() + " (" + classname + ")");
	}
	
	// CREATE LOGGER
	protected void log(String processDefinitionKey, String message, String classname) { }
	
}