package org.cibseven.webapp.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.template.StartFormTemplate;
import org.cibseven.webapp.template.Template;
import org.cibseven.webapp.template.TemplateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RestController @RequestMapping("${services.basePath:/services/v1}" + "/template")
public class TemplateService extends BaseService implements InitializingBean {
  
  private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);
	
	@Autowired
	BaseUserProvider<?> provider;
	@Autowired
	BpmProvider bpmProvider;
	
	Map<String, TemplateTask> name2element = new HashMap<>();
	
	TemplateService(Collection<TemplateTask> elements) {
		for (TemplateTask prov : elements) {
			String type = prov.getClass().getSimpleName();
			TemplateTask old = name2element.put(type, (TemplateTask) prov);
			if (old != null)
				throw new SystemException("Duplicate TemplateProvider for type " + type + ", " + old + " and " + prov);
		}
	}
	
	@Override
	public void afterPropertiesSet() throws Exception { }
	
	@RequestMapping(value = "/{element}/{taskId}", method = RequestMethod.GET)
	public Template getTemplate(@PathVariable String element, @PathVariable String taskId, @RequestParam Optional<String> locale, HttpServletRequest request) throws Exception {
	  CIBUser user = checkAuthorization(request, false, false);
		return name2element.get(element).getTemplate(taskId, locale, user);
	}

	@RequestMapping(value = "/{element}/key/{processDefinitionId}", method = RequestMethod.GET)
	public StartFormTemplate getStartFormTemplate(@PathVariable String element, @PathVariable String processDefinitionId, @RequestParam Optional<String> processDefinitionKey,
			@RequestParam Optional<String> locale, HttpServletRequest request) throws Exception {
	  CIBUser user = checkAuthorization(request, false, false);
//		CIBUser user;
//		try {
//			user = (CIBUser) provider.authenticateUser(rq);
//		} catch(AnonUserBlockedException e) {
//			user = (CIBUser) e.getUser();
//			Authorizations authorizations = bpmProvider.getUserAuthorization(user.getId(), user);
//			provider.hasSpecificProcessRights(authorizations, processDefinitionId);
//		}
		return name2element.get(element).getStartFormTemplate(processDefinitionId, processDefinitionKey, locale, user);
	}
	
}