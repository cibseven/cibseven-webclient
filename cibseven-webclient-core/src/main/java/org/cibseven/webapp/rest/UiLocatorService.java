package org.cibseven.webapp.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.cibseven.templates.uilocator.repository.ExternalUiTaskProvider;
import org.cibseven.templates.uilocator.repository.ExternalUiTaskService;
import org.cibseven.templates.uilocator.repository.MemoryExternalUiTaskProvider;
import lombok.Getter;

@RestController @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}/locator")
public class UiLocatorService extends ExternalUiTaskService {

	@Value("${cibseven.webclient.engineRest.url:./}") protected String cibsevenUrl;
	
	@Getter
	ExternalUiTaskProvider eUiTProvider = new MemoryExternalUiTaskProvider();
	
	public String getEngineRestUrl() {
		return cibsevenUrl + "engine-rest";
	}
}
