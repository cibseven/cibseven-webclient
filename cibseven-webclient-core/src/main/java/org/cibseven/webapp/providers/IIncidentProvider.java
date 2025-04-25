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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Incident;

public interface IIncidentProvider {
	
	public Long countIncident(
			Optional<String> incidentId, // Restricts to incidents that have the given id.
			Optional<String> incidentType, // Restricts to incidents that belong to the given incident type. See the User Guide for a list of incident types.
			Optional<String> incidentMessage, // Restricts to incidents that have the given incident message.
			Optional<String> processDefinitionId, // Restricts to incidents that belong to a process definition with the given id.
			Optional<String> processDefinitionKeyIn, // Restricts to incidents that belong to a process definition with the given Key.
			Optional<String> processInstanceId, // Restricts to incidents that belong to a process instance with the given id.
			Optional<String> executionId, // Restricts to incidents that belong to an execution with the given id.
			Optional<String> activityId, // Restricts to incidents that belong to an activity with the given id.
			Optional<String> causeIncidentId, // Restricts to incidents that have the given incident id as cause incident.
			Optional<String> rootCauseIncidentId, // Restricts to incidents that have the given incident id as root cause incident.
			Optional<String> configuration, // Restricts to incidents that have the given parameter set as configuration.
			Optional<String> tenantIdIn, // Restricts to incidents that have one of the given comma-separated tenant ids.
			Optional<String> jobDefinitionIdIn, // Restricts to incidents that have one of the given comma-separated job definition ids.
			Optional<String> name, // Restricts to incidents that have one of the given name.
			CIBUser user);
	public Collection<Incident> findIncident(
			Optional<String> incidentId, // Restricts to incidents that have the given id.
			Optional<String> incidentType, // Restricts to incidents that belong to the given incident type. See the User Guide for a list of incident types.
			Optional<String> incidentMessage, // Restricts to incidents that have the given incident message.
			Optional<String> processDefinitionId, // Restricts to incidents that belong to a process definition with the given id.
			Optional<String> processDefinitionKeyIn, // Restricts to incidents that belong to a process definition with the given Key.
			Optional<String> processInstanceId, // Restricts to incidents that belong to a process instance with the given id.
			Optional<String> executionId, // Restricts to incidents that belong to an execution with the given id.
			Optional<String> activityId, // Restricts to incidents that belong to an activity with the given id.
			Optional<String> causeIncidentId, // Restricts to incidents that have the given incident id as cause incident.
			Optional<String> rootCauseIncidentId, // Restricts to incidents that have the given incident id as root cause incident.
			Optional<String> configuration, // Restricts to incidents that have the given parameter set as configuration.
			Optional<String> tenantIdIn, // Restricts to incidents that have one of the given comma-separated tenant ids.
			Optional<String> jobDefinitionIdIn, // Restricts to incidents that have one of the given comma-separated job definition ids.			
			CIBUser user);
	public List<Incident> findIncidentByInstanceId(String processInstanceId, CIBUser user);
	public Collection<Incident> fetchIncidents(String processDefinitionKey, CIBUser user);	
	public Collection<Incident> fetchIncidentsByInstanceAndActivityId(String processDefinitionKey, String activityId, CIBUser user);
	public void setIncidentAnnotation(String incidentId, Map<String, Object> data, CIBUser user);
}
