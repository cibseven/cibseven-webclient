package org.cibseven.providers;

import java.util.Arrays;
import java.util.Collection;

import org.cibseven.auth.CIBUser;
import org.cibseven.exception.SystemException;
import org.cibseven.rest.model.Filter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FilterProvider extends SevenProviderBase implements IFilterProvider {

	@Override
	public Collection<Filter> findFilters(CIBUser user) {
		String url = camundaUrl + "/engine-rest/filter?resourceType=Task";
		return Arrays.asList(((ResponseEntity<Filter[]>) doGet(url, Filter[].class, user, false)).getBody());
	}

	@Override
	public Filter createFilter(Filter filter, CIBUser user) {
		String url = camundaUrl + "/engine-rest/filter/create";
		try {
			return ((ResponseEntity<Filter>) doPost(url, filter.json(), Filter.class, user)).getBody();
		} catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in createFilter(...):", se);
			throw se;
		}
	}
	
	@Override
	public void updateFilter(Filter filter, CIBUser user) {
		String url = camundaUrl + "/engine-rest/filter/" + filter.getId();
		try {
			doPut(url, filter.json(), user);
		} catch (JsonProcessingException e) {
			SystemException se = new SystemException(e);
			log.info("Exception in updateFilter(...):", se);
			throw se;
		}
	}

	@Override
	public void deleteFilter(String filterId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/filter/" + filterId;
		doDelete(url, user);
	}

	@Override
	protected HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user) {
		if (user != null) headers.add("Authorization", user.getAuthToken());
		return headers;
	}	
	
}
