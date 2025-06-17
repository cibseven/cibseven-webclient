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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.cibseven.webapp.Data;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.ExpressionEvaluationException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.UnsupportedTypeException;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.cibseven.webapp.rest.model.Incident;
import org.cibseven.webapp.rest.model.Process;
import org.cibseven.webapp.rest.model.ProcessDiagram;
import org.cibseven.webapp.rest.model.ProcessInstance;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.ProcessStatistics;
import org.cibseven.webapp.rest.model.StartForm;
import org.cibseven.webapp.rest.model.TaskSorting;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.providers.utils.URLUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ProcessProvider extends SevenProviderBase implements IProcessProvider{

	 @Autowired private IIncidentProvider incidentProvider;
	
	@Value("${cibseven.webclient.fetchInstances:true}") boolean fetchInstances;
	@Value("${cibseven.webclient.fetchIncidents:true}") boolean fetchIncidents;
	
	@Override
	public Collection<Process> findProcesses(CIBUser user) {
		String url = getEngineRestUrl() + "/process-definition?latestVersion=true&sortBy=name&sortOrder=desc";
		Collection<Process> processes = Arrays.asList(((ResponseEntity<Process[]>) doGet(url, Process[].class, user, false)).getBody());
		return processes;
	}

	@Override
	public Collection<Process> findProcessesWithInfo(CIBUser user) {
		
		Collection<Process> processes = findProcesses(user);
		
		/* The following code slow down the OFDKA system.*/
		for (Process process : processes) {
			if (fetchInstances) {
				String urlInstances = getEngineRestUrl() + "/process-instance/count?processDefinitionKey=" + process.getKey();
				urlInstances += process.getTenantId() != null ? "&tenantIdIn=" + process.getTenantId() : "&withoutTenantId=true";
				process.setRunningInstances(((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody().get("count").asLong());	
			}
			if (fetchIncidents) {
				String urlIncidents = getEngineRestUrl() + "/incident/count?processDefinitionKeyIn=" + process.getKey();
				urlIncidents += process.getTenantId() != null ? "&tenantIdIn=" + process.getTenantId() : "";
				process.setIncidents(((ResponseEntity<JsonNode>) doGet(urlIncidents, JsonNode.class, user, false)).getBody().get("count").asLong());	
			}
		}
		return processes;
	}	

	@Override
	public Collection<Process> findProcessesWithFilters(String filters, CIBUser user) {
		try {
			String url = getEngineRestUrl() + "/process-definition?" + URLDecoder.decode(filters, StandardCharsets.UTF_8.toString());
			Collection<Process> processes = Arrays.asList(((ResponseEntity<Process[]>) doGet(url, Process[].class, user, false)).getBody());
			
			for(Process process : processes) {
				String urlInstances = getEngineRestUrl() + "/process-instance/count?processDefinitionId=" + process.getId();
				process.setRunningInstances(((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody().get("count").asLong());
			}
			
			return processes;
		} catch (Exception e) {
			throw new SystemException(e);
		}
		
	}	
	
	@Override
	public Process findProcessByDefinitionKey(String key, String tenantId, CIBUser user) {
		String url = getEngineRestUrl() + "/process-definition/key/" + key;
		url += tenantId != null ? ("/tenant-id/" + tenantId) : "";
		return ((ResponseEntity<Process>) doGet(url, Process.class, user, false)).getBody();		
	}
	
	@Override
	public Collection<Process> findProcessVersionsByDefinitionKey(String key, String tenantId, Optional<Boolean> lazyLoad, CIBUser user) {
		String url = getEngineRestUrl() + "/process-definition?key=" + key + "&sortBy=version&sortOrder=desc";
		url += tenantId != null ? ("&tenantIdIn=" + tenantId) : "&withoutTenantId=true";
		Collection<Process> processes = Arrays.asList(((ResponseEntity<Process[]>) doGet(url, Process[].class, user, false)).getBody());		
		
		if (!lazyLoad.isPresent() || (lazyLoad.isPresent() && !lazyLoad.get())) {
			for(Process process : processes) {
				String urlInstances = getEngineRestUrl() + "/history/process-instance/count?processDefinitionId=" + process.getId();
				process.setAllInstances(((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody().get("count").asLong());
				urlInstances = getEngineRestUrl() + "/history/process-instance/count?unfinished=true&processDefinitionId=" + process.getId();
				process.setRunningInstances(((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody().get("count").asLong());
				urlInstances = getEngineRestUrl() + "/history/process-instance/count?completed=true&processDefinitionId=" + process.getId();
				process.setCompletedInstances(((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody().get("count").asLong());
			}
		}
		return processes;
	}
	
	@Override
	public Process findProcessById(String id, Optional<Boolean> extraInfo, CIBUser user) throws SystemException {
		String url = getEngineRestUrl() + "/process-definition/" + id;
		Process process = ((ResponseEntity<Process>) doGet(url, Process.class, user, false)).getBody();
		
		if (extraInfo.isPresent() && extraInfo.get()) {
			String urlInstances = getEngineRestUrl() + "/history/process-instance/count?processDefinitionId=" + id;
			process.setAllInstances(((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody().get("count").asLong());
			urlInstances = getEngineRestUrl() + "/history/process-instance/count?unfinished=true&processDefinitionId=" + process.getId();
			process.setRunningInstances(((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody().get("count").asLong());
			urlInstances = getEngineRestUrl() + "/history/process-instance/count?completed=true&processDefinitionId=" + process.getId();
			process.setCompletedInstances(((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody().get("count").asLong());
		}
		
		return process;
	}
	
	@Override
	public Collection<ProcessInstance> findProcessesInstances(String key, CIBUser user) {
		String url = getEngineRestUrl() + "/process-instance?processDefinitionKey=" + key;
		return Arrays.asList(((ResponseEntity<ProcessInstance[]>) doGet(url, ProcessInstance[].class, user, false)).getBody());	
	}
	
	@Override
	public Collection<ProcessInstance> findCurrentProcessesInstances(Map<String, Object> data, CIBUser user) {
		String url = getEngineRestUrl() + "/process-instance";
		return Arrays.asList(((ResponseEntity<ProcessInstance[]>) doPost(url, data, ProcessInstance[].class, user)).getBody());
	}
	
	@Override
	public ProcessDiagram fetchDiagram(String id, CIBUser user) {
		String url = getEngineRestUrl() + "/process-definition/" + id + "/xml";
		return ((ResponseEntity<ProcessDiagram>) doGet(url, ProcessDiagram.class, user, false)).getBody();
	}
	
	@Override
	public StartForm fetchStartForm(String processDefinitionId, CIBUser user) {
		String url = getEngineRestUrl() + "/process-definition/" + processDefinitionId + "/startForm";
		return ((ResponseEntity<StartForm>) doGet(url, StartForm.class, user, false)).getBody();
	}
	
	@Override
	public Data downloadBpmn(String id, String fileName, CIBUser user) {
		ProcessDiagram diagram = fetchDiagram(id, user);
			ByteArrayResource resource = new ByteArrayResource(diagram.getBpmn20Xml().getBytes());
			return new Data(fileName, "application/bpmn+xml", resource, resource.contentLength());
	}
	
	@Override
	public void suspendProcessInstance(String processInstanceId, Boolean suspend, CIBUser user) {
		String url = getEngineRestUrl() + "/process-instance/" + processInstanceId + "/suspended";
		doPut(url, "{ \"suspended\": " + suspend + " }", user);
	}
	
	@Override
	public void deleteProcessInstance(String processInstanceId, CIBUser user) {
		String url = getEngineRestUrl() + "/process-instance/" + processInstanceId;
		doDelete(url, user);
	}
	
	@Override
	public void suspendProcessDefinition(String processDefinitionId, Boolean suspend, Boolean includeProcessInstances, String executionDate, CIBUser user) {
		String url = getEngineRestUrl() + "/process-definition/" + processDefinitionId + "/suspended";
		doPut(url, "{ "
				+ "\"suspended\": " + suspend + ","
				+ "\"includeProcessInstances\": " + includeProcessInstances + ","
				+ "\"executionDate\": " + executionDate 
				+ " }", user);
	}	

	@Override
	public ProcessStart startProcess(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		String url = getEngineRestUrl() + "/process-definition/key/" + processDefinitionKey;
		url += (tenantId != null ? ("/tenant-id/" + tenantId) : "") + "/start";
		return ((ResponseEntity<ProcessStart>) doPost(url, data, ProcessStart.class, user)).getBody();
	}
	
	@Override
	public ProcessStart submitForm(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		// Used by Webdesk
		String url = getEngineRestUrl() + "/process-definition/key/" + processDefinitionKey;
		url += (tenantId != null ? ("/tenant-id/" + tenantId) : "") + "/submit-form";
		return ((ResponseEntity<ProcessStart>) doPost(url, data, ProcessStart.class, user)).getBody();
	}
	
	@Override
	public Collection<ProcessStatistics> findProcessStatistics(String processId, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		String url = getEngineRestUrl() + "/process-definition/" + processId + "/statistics?failedJobs=true";
		return Arrays.asList(((ResponseEntity<ProcessStatistics[]>) doGet(url, ProcessStatistics[].class, user, false)).getBody());
	}

  @Override
  public Collection<ProcessStatistics> getProcessStatistics(CIBUser user) {
    String url = getEngineRestUrl() + "/process-definition/statistics?failedJobs=true&rootIncidents=true";
    return Arrays.asList(((ResponseEntity<ProcessStatistics[]>) doGet(url, ProcessStatistics[].class, user, false)).getBody());
  }
	
	@Override
	public HistoryProcessInstance findHistoryProcessInstanceHistory(String processInstanceId, CIBUser user) {
		String url = getEngineRestUrl() + "/history/process-instance/" + processInstanceId;
		return ((ResponseEntity<HistoryProcessInstance>) doGet(url, HistoryProcessInstance.class, user, false)).getBody();
	}
	
	@Override
	public Collection<HistoryProcessInstance> findProcessesInstancesHistory(Map<String, Object> data,
			Optional<Integer> firstResult, Optional<Integer> maxResults, CIBUser user) {
		Map<String, Object> queryParams = new HashMap<String, Object>();
		if (firstResult.isPresent()) queryParams.put("firstResult", firstResult.get());
		if (maxResults.isPresent()) queryParams.put("maxResults", maxResults.get());
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl() + "/history/process-instance", queryParams);
		return Arrays.asList(((ResponseEntity<HistoryProcessInstance[]>) doPost(url, data, HistoryProcessInstance[].class, user)).getBody());
	}
	
	@Override
	public Collection<HistoryProcessInstance> findProcessesInstancesHistory(String key, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, CIBUser user) {
		String url = getEngineRestUrl() + "/history/process-instance?processDefinitionKey=" + key;
		if (active.isPresent()) {
			url += (active.get()) ? "&unfinished=true" : "&finished=true";
		}
		url += "&firstResult=" + firstResult + "&maxResults=" + maxResults;
		return Arrays.asList(((ResponseEntity<HistoryProcessInstance[]>) doGet(url, HistoryProcessInstance[].class, user, false)).getBody());	
	}
	
	@Override
	public Collection<HistoryProcessInstance> findProcessesInstancesHistoryById(String id, Optional<String> activityId, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, String text, CIBUser user) {
		String url = getEngineRestUrl() + "/history/process-instance?processDefinitionId=" + id;
		Map<String, Object> data = new HashMap<String, Object>();
		List<TaskSorting> sorting = Arrays.asList(new TaskSorting("startTime", "desc"));
		data.put("sorting", sorting);
		if (text != "") {
			Map<String, Object> orQueries = new HashMap<String, Object>();
			orQueries.put("processInstanceBusinessKeyLike", "%" + text + "%");
			orQueries.put("processInstanceId", text);
			data.put("orQueries", Arrays.asList(orQueries));
		}
		if (active.isPresent()) {
			url += (active.get()) ? data.put("unfinished", true) : data.put("finished", true);
		}
		if (activityId.isPresent() && !activityId.get().isEmpty()) {
			data.put("activeActivityIdIn", Arrays.asList(activityId.get()));
		}
		url += "&firstResult=" + firstResult + "&maxResults=" + maxResults;
		data.put("processDefinitionId", id);
		ObjectMapper objectMapper = new ObjectMapper();
		String body = "";
		try {
			body = objectMapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			throw new SystemException(e);
		}
		//findIncident
		Collection<HistoryProcessInstance> processes = Arrays.asList(((ResponseEntity<HistoryProcessInstance[]>) doPost(url, body, HistoryProcessInstance[].class, user)).getBody());
		
		if ((processes == null || processes.isEmpty()) && activityId.isPresent() && !activityId.get().isEmpty()) {
			Collection<Incident> incidents = incidentProvider.fetchIncidentsByInstanceAndActivityId(id, activityId.get(), user);
		    if (incidents != null && !incidents.isEmpty()) {
		        Map<String, List<Incident>> incidentsByProcessInstance = incidents.stream()
		            .collect(Collectors.groupingBy(Incident::getProcessInstanceId));
		        
		        Set<String> processInstanceIds = incidentsByProcessInstance.keySet();
		        
		        Map<String, Object> dataIdIn = new HashMap<>();
		        dataIdIn.put("processInstanceIdIn", processInstanceIds);
		        dataIdIn.put("processDefinitionId", id);
		        dataIdIn.put("firstResult", firstResult);
		        dataIdIn.put("maxResults", maxResults);
		        dataIdIn.put("sorting", sorting);
		        
		        String bodyIdIn = "";
		        try {
		        	bodyIdIn = objectMapper.writeValueAsString(dataIdIn);
		        } catch (JsonProcessingException e) {
		            throw new SystemException(e);
		        }
		        
		        processes = Arrays.asList(
		            ((ResponseEntity<HistoryProcessInstance[]>) doPost(url, bodyIdIn, HistoryProcessInstance[].class, user)).getBody()
		        );
		        processes.forEach(p -> p.setIncidents(incidentsByProcessInstance.getOrDefault(p.getId(), Collections.emptyList())));
		    }
		} else {		
			processes.forEach(p -> {
				p.setIncidents(incidentProvider.findIncidentByInstanceId(p.getId(), user));
			});
		}
			
		return processes;
	}

	@Override
	public Long countProcessesInstancesHistory(Map<String, Object> filters, CIBUser user) {
		String url = getEngineRestUrl() + "/history/process-instance/count";
		return ((ResponseEntity<JsonNode>) doPost(url, filters, JsonNode.class, user)).getBody().get("count").asLong();
	}
	
	@Override
	public ProcessInstance findProcessInstance(String processInstanceId, CIBUser user) {
		String url = getEngineRestUrl() + "/process-instance/" + processInstanceId;
		return ((ResponseEntity<ProcessInstance>) doGet(url, ProcessInstance.class, user, false)).getBody();
	}
	
	@Override
	public Variable fetchProcessInstanceVariable(String processInstanceId, String variableName, String deserializeValue, CIBUser user) throws SystemException  {
		String url = getEngineRestUrl() + "/process-instance/" + processInstanceId + "/variables/" + variableName;
		url += StringUtils.isEmpty(deserializeValue) ? "" : "?deserializeValue=" + deserializeValue;
		return ((ResponseEntity<Variable>) doGet(url, Variable.class, null, false)).getBody();
	}
	
	@Override
	public Collection<Process> findCalledProcessDefinitions(String processDefinitionId, CIBUser user) {
		String url = getEngineRestUrl() + "/process-definition/" + processDefinitionId + "/static-called-process-definitions";
		
		return Arrays.asList(((ResponseEntity<Process[]>) doGet(url, Process[].class, user, false)).getBody());
	}
	
	@Override
	public ResponseEntity<byte[]> getDeployedStartForm(String processDefinitionId, CIBUser user) {
		String url = getEngineRestUrl() + "/process-definition/" + processDefinitionId + "/deployed-start-form";
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
	    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		if (user != null) headers.add("Authorization", user.getAuthToken());
		HttpEntity<String> entity = new HttpEntity<String>(headers);
	    ResponseEntity<byte[]> response = restTemplate.exchange(builder.build().toUriString(), HttpMethod.GET, entity, byte[].class, "1");
		return response;
	}

	@Override
	public void updateHistoryTimeToLive(String id, Map<String, Object> data, CIBUser user) {
		try {
			String url = getEngineRestUrl() + "/process-definition/" + id + "/history-time-to-live";
			doPut(url, data, user);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	@Override
	public void deleteProcessInstanceFromHistory(String id, CIBUser user) {
		String url = getEngineRestUrl() + "/history/process-instance/" + id;
		doDelete(url, user);
	}
	
	@Override
	public void deleteProcessDefinition(String id, Optional<Boolean> cascade, CIBUser user) {
		boolean cascadeVal = cascade.orElse(true);
		String url = getEngineRestUrl() + "/process-definition/" + id + "?cascade=" + cascadeVal;
		doDelete(url, user);
	}

}