/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cibseven.webapp.providers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.EventSubscription;
import org.cibseven.webapp.rest.model.Message;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class UtilsProvider extends SevenProviderBase implements IUtilsProvider {
	
	@Override
	public Collection<Message> correlateMessage(Map<String, Object> data, CIBUser user) throws SystemException {
		//LHM overlay (webdesk)
		String url = camundaUrl + "/engine-rest/message";
		ResponseEntity<Message[]> response = doPost(url, data, Message[].class, user);
		if (response.hasBody()) {
			return Arrays.asList(response.getBody());			
		}
		else {
			return null;
		}
	}
	
	@Override
	public String findStacktrace(String jobId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/job/" + jobId	+ "/stacktrace";
		
		return doGetWithHeader(url, String.class, user, false, MediaType.ALL).getBody();
	}
	
	@Override
	public void retryJobById(String jobId, Map<String, Object> data, CIBUser user) {
		String url = camundaUrl + "/engine-rest/job/" + jobId + "/retries";
		doPut(url, data, user);
	}
	
	@Override
	public Collection<EventSubscription> getEventSubscriptions(Optional<String> processInstanceId,
			Optional<String> eventType, Optional<String> eventName, CIBUser user) {
		String url = camundaUrl + "/engine-rest/event-subscription";
		String param = "";
		param += addQueryParameter(param, "processInstanceId", processInstanceId, true);
		param += addQueryParameter(param, "eventType", eventType, true);
		param += addQueryParameter(param, "eventName", eventName, true);
		url += param;
		return Arrays.asList(((ResponseEntity<EventSubscription[]>) doGet(url, EventSubscription[].class, user, false)).getBody());
	}
	
}
