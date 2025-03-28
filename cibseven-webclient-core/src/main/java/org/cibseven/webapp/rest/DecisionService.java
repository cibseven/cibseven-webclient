package org.cibseven.webapp.rest;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.Decision;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController @RequestMapping("${services.basePath:/services/v1}" + "/decision")
public class DecisionService extends BaseService implements InitializingBean {
	
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else throw new SystemException("ProcessService expects a BpmProvider");
	}
	
	@Operation(
			summary = "Queries for decision definitions that fulfill given parameters. "
					+ "Parameters may be the properties of decision definitions, such as the name, key or version.",
			description = "<strong>Return: Collection of decisions")
	@ApiResponse(responseCode = "400", description = "There is at least one invalid parameter value")
	@GetMapping
	public Collection<Decision> getList(@RequestParam Map<String, Object> queryParams, Locale loc) {
		return bpmProvider.getDecisionDefinitionList(queryParams);
	}
	
	@GetMapping("/count")
	public Object getDecisionDefinitionListCount(@RequestParam Map<String, Object> queryParams) {
		return bpmProvider.getDecisionDefinitionListCount(queryParams);
	}

	@GetMapping("/key/{key}")
	public Decision getDecisionDefinitionByKey(@PathVariable String key) {
		return bpmProvider.getDecisionDefinitionByKey(key);
	}

	@GetMapping("/key/{key}/diagram")
	public Object getDiagramByKey(@PathVariable String key) {
		return bpmProvider.getDiagramByKey(key);
	}

	@PostMapping("/key/{key}/evaluate")
	public Object evaluateDecisionDefinitionByKey(@RequestBody Map<String, Object> data, @PathVariable String key, CIBUser user) {
		return bpmProvider.evaluateDecisionDefinitionByKey(data, key, user);
	}

	@PutMapping("/key/{key}/history-ttl")
	public void updateHistoryTTLByKey(@RequestBody Map<String, Object> data, @PathVariable String key, CIBUser user) {
		bpmProvider.updateHistoryTTLByKey(data, key, user);
	}

	@GetMapping("/key/{key}/tenant/{tenant}")
	public Decision getDecisionDefinitionByKeyAndTenant(@PathVariable String key, @PathVariable String tenant) {
		return bpmProvider.getDecisionDefinitionByKeyAndTenant(key, tenant);
	}

	@GetMapping("/key/{key}/tenant/{tenant}/diagram")
	public Object getDiagramByKeyAndTenant(@PathVariable String key, @PathVariable String tenant) {
		return bpmProvider.getDiagramByKeyAndTenant(key, tenant);
	}

	@PostMapping("/key/{key}/tenant/{tenant}/evaluate")
	public Object evaluateDecisionDefinitionByKeyAndTenant(@PathVariable String key, @PathVariable String tenant) {
		return bpmProvider.evaluateDecisionDefinitionByKeyAndTenant(key, tenant);
	}

	@PutMapping("/key/{key}/tenant/{tenant}/history-ttl")
	public Object updateHistoryTTLByKeyAndTenant(@PathVariable String key, @PathVariable String tenant) {
		return bpmProvider.updateHistoryTTLByKeyAndTenant(key, tenant);
	}

	@GetMapping("/key/{key}/xml")
	public Object getXmlByKey(@PathVariable String key) {
		return bpmProvider.getXmlByKey(key);
	}

	@GetMapping("/key/{key}/tenant/{tenant}/xml")
	public Object getXmlByKeyAndTenant(@PathVariable String key, @PathVariable String tenant) {
		return bpmProvider.getXmlByKeyAndTenant(key, tenant);
	}

	@GetMapping("/id/{id}")
	public Decision getDecisionDefinitionById(@PathVariable String id) {
		return bpmProvider.getDecisionDefinitionById(id);
	}

	@GetMapping("/id/{id}/diagram")
	public Object getDiagramById(@PathVariable String id) {
		return bpmProvider.getDiagramById(id);
	}

	@PostMapping("/id/{id}/evaluate")
	public Object evaluateDecisionDefinitionById(@PathVariable String id) {
		return bpmProvider.evaluateDecisionDefinitionById(id);
	}

	@PutMapping("/id/{id}/history-ttl")
	public Object updateHistoryTTLById(@PathVariable String id) {
		return bpmProvider.updateHistoryTTLById(id);
	}

	@GetMapping("/id/{id}/xml")
	public Object getXmlById(@PathVariable String id) {
		return bpmProvider.getXmlById(id);
	}
	
}