package org.cibseven.webapp.rest;

import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.SevenProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({
	@ApiResponse(responseCode= "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode= "401", description = "Unauthorized")
})
@RestController @RequestMapping("${services.basePath:/services/v1}" + "/system")
public class SystemService extends BaseService implements InitializingBean {

	@Autowired BpmProvider bpmProvider;
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else throw new SystemException("SystemService expects a BpmProvider");
	}

	// @GetMapping("/metrics")
	// public Object getMetrics(@RequestParam Map<String, Object> queryParams, CIBUser user) {
	// 	return bpmProvider.getMetrics(queryParams, user);
	// }

	// @GetMapping("/metrics/{metricName}/sum")
	// public Object getMetricsSum(@PathVariable String metricName, 
	// 		@RequestParam Map<String, Object> queryParams, CIBUser user) {
	// 	return bpmProvider.getMetricsSum(metricName, queryParams, user);
	// }

	@GetMapping("/telemetry/data")
	public JsonNode getTelemetryData(CIBUser user) {
		return bpmProvider.getTelemetryData(user);
	}

	
}
