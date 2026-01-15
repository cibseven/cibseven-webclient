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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.cibseven.webapp.Data;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.ExpressionEvaluationException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.UnsupportedTypeException;
import org.cibseven.webapp.rest.model.Process;
import org.cibseven.webapp.rest.model.ProcessDiagram;
import org.cibseven.webapp.rest.model.ProcessInstance;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.ProcessStatistics;
import org.cibseven.webapp.rest.model.StartForm;
import org.cibseven.webapp.rest.model.Variable;
import org.springframework.http.ResponseEntity;

import org.cibseven.webapp.rest.model.IncidentInfo;
import org.cibseven.webapp.rest.model.KeyTenant;

public interface IProcessProvider {
	
	public Collection<Process> findProcesses(CIBUser user);
	public Collection<Process> findProcessesWithInfo(CIBUser user);
	public Collection<Process> findProcessesWithFilters(String filters, CIBUser user);
	public Process findProcessByDefinitionKey(String key, String tenantId, CIBUser user);
	public Collection<Process> findProcessVersionsByDefinitionKey(String key, String tenantId, Optional<Boolean> lazyLoad, CIBUser user);
	public Process findProcessById(String id, Optional<Boolean> extraInfo, CIBUser user) throws SystemException;
	public Collection<ProcessInstance> findProcessesInstances(String key, CIBUser user);
	public Collection<ProcessInstance> findCurrentProcessesInstances(Map<String, Object> data, CIBUser user);
	public ProcessDiagram fetchDiagram(String id, CIBUser user);
	public StartForm fetchStartForm(String processDefinitionId, CIBUser user);
	public Data downloadBpmn(String id, String fileName, CIBUser user);
	public void suspendProcessInstance(String processInstanceId, Boolean suspend, CIBUser user);
	public void deleteProcessInstance(String processInstanceId, CIBUser user);
	public void suspendProcessDefinition(String processDefinitionId, Boolean suspend, Boolean includeProcessInstances, String executionDate, CIBUser user);
	public ProcessStart startProcess(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException;
	public ProcessStart submitForm(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException;
	public ProcessStart submitForm(String processDefinitionKey, String formResult, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException;
	public Collection<ProcessStatistics> findProcessStatistics(String processId, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException;
	public Collection<ProcessStatistics> getProcessStatistics(Map<String, Object> queryParams, CIBUser user);
	public List<ProcessStatistics> groupProcessStatisticsByKeyAndTenant(Collection<ProcessStatistics> processStatistics);
	public HistoryProcessInstance findHistoryProcessInstanceHistory(String processInstanceId, CIBUser user);
	public Collection<HistoryProcessInstance> findProcessesInstancesHistory(Map<String, Object> filters,
			Optional<Integer> firstResult, Optional<Integer> maxResults, CIBUser user);
	public Collection<HistoryProcessInstance> findProcessesInstancesHistory(String key, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, CIBUser user);
	public Collection<HistoryProcessInstance> findProcessesInstancesHistoryById(String id, Optional<String> activityId, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, String text, CIBUser user);
	public ProcessInstance findProcessInstance(String processInstanceId, CIBUser user);
	public Variable fetchProcessInstanceVariable(String processInstanceId, String variableName, boolean deserializeValue, CIBUser user) throws SystemException;
	public Collection<Process> findCalledProcessDefinitions(String processDefinitionId, CIBUser user);
	public ResponseEntity<byte[]> getDeployedStartForm(String processDefinitionId, CIBUser user);
	public ResponseEntity<String> getRenderedForm(String processDefinitionId, Map<String, Object> params, CIBUser user);
		
	public void updateHistoryTimeToLive(String id, Map<String, Object> data, CIBUser user);
	public void deleteProcessInstanceFromHistory(String id, CIBUser user);
	public void deleteProcessDefinition(String id, Optional<Boolean> cascade, CIBUser user);
	public Long countProcessesInstancesHistory(Map<String, Object> filters, CIBUser user);
	public Object fetchHistoricActivityStatistics(String id, Map<String, Object> params, CIBUser user);
	
	default List<ProcessStatistics> groupProcessStatisticsByKeyAndTenantImpl(Collection<ProcessStatistics> processStatistics) {
        return processStatistics.stream()
            .collect(Collectors.groupingBy(
                stat -> new KeyTenant(stat.getDefinition().getKey(), stat.getDefinition().getTenantId())
            ))
            .values()
            .stream()
            .map(group -> {
                ProcessStatistics result = new ProcessStatistics();

                // Sort by version descending and use the latest version's definition
                ProcessStatistics latestVersion = group.stream()
                    .max(Comparator.comparing(stat -> stat.getDefinition().getVersion()))
                    .orElse(group.iterator().next());

                result.setDefinition(latestVersion.getDefinition());
                result.setId(latestVersion.getId());

                // Aggregate instances and failed jobs
                result.setInstances(group.stream().mapToLong(ProcessStatistics::getInstances).sum());
                result.setFailedJobs(group.stream().mapToLong(ProcessStatistics::getFailedJobs).sum());

                // Aggregate incidents
                long totalIncidentCount = group.stream()
                    .flatMap(stat -> stat.getIncidents().stream())
                    .mapToLong(IncidentInfo::getIncidentCount)
                    .sum();

                IncidentInfo totalIncident = new IncidentInfo();
                totalIncident.setIncidentType("all");
                totalIncident.setIncidentCount(totalIncidentCount);

                result.setIncidents(Collections.singletonList(totalIncident));

                return result;
            })
            .collect(Collectors.toList());
    }
}