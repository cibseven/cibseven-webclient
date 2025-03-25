package org.cibseven.webapp.template;

import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;


public interface TemplateTask {
	
	Template getTemplate(String taskId, Optional<String> locale, CIBUser user) throws Exception;
	StartFormTemplate getStartFormTemplate(String processDefinitionId, Optional<String> processDefinitionKey, Optional<String> locale, CIBUser user) throws Exception;
	
}