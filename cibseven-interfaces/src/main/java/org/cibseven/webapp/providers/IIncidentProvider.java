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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Incident;

public interface IIncidentProvider {
	
	public Long countIncident(Map<String, Object> params, CIBUser user);
	public Long countHistoricIncident(Map<String, Object> params, CIBUser user);

	public Collection<Incident> findIncident(Map<String, Object> params, CIBUser user);
	public List<Incident> findIncidentByInstanceId(String processInstanceId, CIBUser user);
	public Collection<Incident> fetchIncidents(String processDefinitionKey, CIBUser user);	
	public Collection<Incident> fetchIncidentsByInstanceAndActivityId(String processDefinitionKey, String activityId, CIBUser user);
	public void setIncidentAnnotation(String incidentId, Map<String, Object> data, CIBUser user);
	public void retryExternalTask(String externalTaskId, Map<String, Object> data, CIBUser user);
	public String findExternalTaskErrorDetails(String externalTaskId, CIBUser user);
	public String findHistoricExternalTaskErrorDetails(String externalTaskId, CIBUser user);
	public Collection<Incident> findHistoricIncidents(Map<String, Object> params, CIBUser user);
	public String findHistoricStacktraceByJobId(String jobId, CIBUser user);
}
