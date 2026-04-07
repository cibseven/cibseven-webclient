package org.cibseven.webapp.rest;

import org.cibseven.templates.uilocator.repository.ExternalUiTaskService;
import org.cibseven.templates.uilocator.repository.MemoryExternalUiTaskProvider;
import org.cibseven.templates.uilocator.repository.SevenEngineRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}/locator")
public class UiLocatorService extends ExternalUiTaskService {
	    public UiLocatorService(
        @Value("${cibseven.webclient.engineRest.url}") String cibsevenUrl, 
		@Value("${cibseven.webclient.engineRest.path:/engine-rest}") String engineRestPath) {
        	super(
            new SevenEngineRestClient(cibsevenUrl + engineRestPath),
            new MemoryExternalUiTaskProvider()
        );
	}
	
		/* 
	@Value("${cibseven.webclient.engineRest.url:./}") protected String cibsevenUrl;
	
	@Getter
	ExternalUiTaskProvider eUiTProvider = new MemoryExternalUiTaskProvider();
	
	public String getEngineRestUrl() {
		return cibsevenUrl + "engine-rest";
		}*/

}