/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.cibseven.webapp.providers;

import java.util.Arrays;
import java.util.Collection;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Filter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FilterProvider extends SevenProviderBase implements IFilterProvider {

	@Override
	public Collection<Filter> findFilters(CIBUser user) {
		String url = cibsevenUrl + "/engine-rest/filter?resourceType=Task";
		return Arrays.asList(((ResponseEntity<Filter[]>) doGet(url, Filter[].class, user, false)).getBody());
	}

	@Override
	public Filter createFilter(Filter filter, CIBUser user) {
		String url = cibsevenUrl + "/engine-rest/filter/create";
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
		String url = cibsevenUrl + "/engine-rest/filter/" + filter.getId();
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
		String url = cibsevenUrl + "/engine-rest/filter/" + filterId;
		doDelete(url, user);
	}

}
