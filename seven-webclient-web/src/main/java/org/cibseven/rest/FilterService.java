package org.cibseven.rest;

import java.util.Collection;
import java.util.Locale;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.cib.cibflow.api.rest.camunda.model.Filter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import de.cib.cibflow.CIBFlowUser;

@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController @RequestMapping("/flow-engine/filter")
public class FilterService extends BaseService{
	
	@Operation(
			summary = "Get collections of filters",
			description = "Get all filters" + "<br>" +
			"<strong>Return: Collection filters fetched in the search")
	@RequestMapping(method = RequestMethod.GET)
	public Collection<Filter> findFilter(
			Locale loc, CIBFlowUser user) {
		return bpmProvider.findFilters(user);
	}
	
	@Operation(
			summary = "Create filter",
			description = "Request body: A JSON object with the following properties: id, resourceType, name, owner, query and properties" + "<br>" +
			"<strong>Return: Filter")
	@RequestMapping(method = RequestMethod.POST)
	public Filter createFilter(
			@RequestBody Filter filter,
			Locale loc, CIBFlowUser user) {
		return bpmProvider.createFilter(filter, user);
	}
	
	@Operation(
			summary = "Update filter",
			description = "Request body: A JSON object with the following properties: id, resourceType, name, owner, query and properties" + "<br>" +
			"<strong>Return: void")
	@RequestMapping(method = RequestMethod.PUT)
	public void updateFilter(
			@RequestBody Filter filter,
			Locale loc, CIBFlowUser user) {
		bpmProvider.updateFilter(filter, user);
	}
	
	@Operation(
			summary = "Delete filter",
			description = "<strong>Return: void")
	@ApiResponse(responseCode = "404", description = "Filter not found")
	@RequestMapping(value = "/{filterId}", method = RequestMethod.DELETE)
	public void deleteFilter(
			@Parameter(description = "Filter Id") @PathVariable String filterId,
			Locale loc, CIBFlowUser user) {
		bpmProvider.deleteFilter(filterId, user);
	}
	
}
