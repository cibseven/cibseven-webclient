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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
import java.util.Comparator;

import org.cibseven.webapp.Data;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.ExpressionEvaluationException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.UnsupportedTypeException;
import org.cibseven.webapp.rest.model.HistoryProcessInstance;
import org.cibseven.webapp.rest.model.Incident;
import org.cibseven.webapp.rest.model.IncidentInfo;
import org.cibseven.webapp.rest.model.KeyTenant;
import org.cibseven.webapp.rest.model.Process;
import org.cibseven.webapp.rest.model.ProcessDefinitionInfo;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
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
		String url = getEngineRestUrl(user) + "/process-definition?latestVersion=true&sortBy=name&sortOrder=desc";
		Collection<Process> processes = Arrays.asList(((ResponseEntity<Process[]>) doGet(url, Process[].class, user, false)).getBody());
		return processes;
	}

	@Override
	public Collection<Process> findProcessesWithInfo(CIBUser user) {

		// Get statistics for all process definitions in one call
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("failedJobs", true);
		queryParams.put("incidents", true);
		Collection<ProcessStatistics> statisticsCollection = getProcessStatistics(queryParams, user);

		// Group by key and tenant ID to consolidate different versions
		List<ProcessStatistics> groupedStatistics = groupProcessStatisticsByKeyAndTenant(statisticsCollection);

		// Build Process objects directly from grouped ProcessStatistics
		return groupedStatistics.stream()
				.map(stats -> {
					Process process = new Process();
					ProcessDefinitionInfo definition = stats.getDefinition();

					// Copy fields from ProcessDefinitionInfo
					if (definition != null) {
						process.setId(definition.getId());
						process.setKey(definition.getKey());
						process.setCategory(definition.getCategory());
						process.setDescription(definition.getDescription());
						process.setName(definition.getName());
						process.setVersion(definition.getVersion() != null ? definition.getVersion().toString() : null);
						process.setResource(definition.getResource());
						process.setDeploymentId(definition.getDeploymentId());
						process.setDiagram(definition.getDiagram());
						process.setSuspended(definition.getSuspended() != null ? definition.getSuspended().toString() : null);
						process.setTenantId(definition.getTenantId());
						process.setVersionTag(definition.getVersionTag());
						process.setHistoryTimeToLive(definition.getHistoryTimeToLive() != null ? definition.getHistoryTimeToLive().toString() : null);
						process.setStartableInTasklist(definition.getStartableInTasklist());
					}

					// Set aggregated statistics data
					process.setRunningInstances(stats.getInstances());
					// Calculate total incidents from all incident types
					long totalIncidents = stats.getIncidents() != null 
						? stats.getIncidents().stream().mapToLong(incident -> incident.getIncidentCount()).sum()
						: 0L;
					process.setIncidents(totalIncidents);

					// Set default values for fields not available in statistics
					process.setAllInstances(stats.getInstances()); // Same as running instances for now
					process.setCompletedInstances(0L); // Would need separate call to get completed instances

					return process;
				})
				.collect(Collectors.toList());
	}	

	@Override
	public Collection<Process> findProcessesWithFilters(String filters, CIBUser user) {
		try {
			String url = getEngineRestUrl(user) + "/process-definition?" + URLDecoder.decode(filters, StandardCharsets.UTF_8.toString());
			Collection<Process> processes = Arrays.asList(((ResponseEntity<Process[]>) doGet(url, Process[].class, user, false)).getBody());

			for(Process process : processes) {
				String urlInstances = getEngineRestUrl(user) + "/process-instance/count?processDefinitionId=" + process.getId();
				JsonNode body = ((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody();
				if (body == null) {
					throw new NullPointerException();
				}
				process.setRunningInstances(body.get("count").asLong());
			}

			return processes;
		} catch (Exception e) {
			throw new SystemException(e);
		}

	}	

	@Override
	public Process findProcessByDefinitionKey(String key, String tenantId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/process-definition/key/" + key;
		url += tenantId != null ? ("/tenant-id/" + tenantId) : "";
		return ((ResponseEntity<Process>) doGet(url, Process.class, user, false)).getBody();		
	}

	@Override
	public Collection<Process> findProcessVersionsByDefinitionKey(String key, String tenantId, Optional<Boolean> lazyLoad, CIBUser user) {
		String url = getEngineRestUrl(user) + "/process-definition?key=" + key + "&sortBy=version&sortOrder=desc";
		url += tenantId != null ? ("&tenantIdIn=" + tenantId) : "&withoutTenantId=true";
		Collection<Process> processes = Arrays.asList(((ResponseEntity<Process[]>) doGet(url, Process[].class, user, false)).getBody());		

		if (!lazyLoad.isPresent() || (lazyLoad.isPresent() && !lazyLoad.get())) {
			for(Process process : processes) {
				String urlInstances = getEngineRestUrl(user) + "/history/process-instance/count?processDefinitionId=" + process.getId();

				JsonNode body = ((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody();
				if (body == null)
					throw new NullPointerException();
				process.setAllInstances(body.get("count").asLong());

				urlInstances = getEngineRestUrl(user) + "/history/process-instance/count?unfinished=true&processDefinitionId=" + process.getId();

				body = ((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody();
				if (body == null)
					throw new NullPointerException();
				process.setRunningInstances(body.get("count").asLong());

				urlInstances = getEngineRestUrl(user) + "/history/process-instance/count?completed=true&processDefinitionId=" + process.getId();
				body = ((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody();
				if (body == null)
					throw new NullPointerException();
				process.setCompletedInstances(body.get("count").asLong());
			}
		}
		return processes;
	}

	@Override
	public Process findProcessById(String id, Optional<Boolean> extraInfo, CIBUser user) throws SystemException {
		String url = getEngineRestUrl(user) + "/process-definition/" + id;
		Process process = ((ResponseEntity<Process>) doGet(url, Process.class, user, false)).getBody();
		if (process == null)
			throw new NullPointerException();

		if (extraInfo.isPresent() && extraInfo.get()) {

			// all instances
			String urlInstances = getEngineRestUrl(user) + "/history/process-instance/count?processDefinitionId=" + id;
			JsonNode body = ((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody();
			if (body == null)
				throw new NullPointerException();
			process.setAllInstances(body.get("count").asLong());

			// running instances
			// should be fetched from runtime api, due to:
			// - when historyLevel is none, no history is recorded
			// - it could be faster when a lot of instances are completed in the past
			urlInstances = getEngineRestUrl(user) + "/process-instance/count?processDefinitionId=" + process.getId();
			body = ((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody();;
			if (body == null)
				throw new NullPointerException();
			process.setRunningInstances(body.get("count").asLong());

			// completed instances
			urlInstances = getEngineRestUrl(user) + "/history/process-instance/count?completed=true&processDefinitionId=" + process.getId();
			body = ((ResponseEntity<JsonNode>) doGet(urlInstances, JsonNode.class, user, false)).getBody();;
			if (body == null)
				throw new NullPointerException();
			process.setCompletedInstances(body.get("count").asLong());
		}

		return process;
	}

	@Override
	public Collection<ProcessInstance> findProcessesInstances(String key, CIBUser user) {
		String url = getEngineRestUrl(user) + "/process-instance?processDefinitionKey=" + key;
		return Arrays.asList(((ResponseEntity<ProcessInstance[]>) doGet(url, ProcessInstance[].class, user, false)).getBody());	
	}

	@Override
	public Collection<ProcessInstance> findCurrentProcessesInstances(Map<String, Object> data, CIBUser user) {
		String url = getEngineRestUrl(user) + "/process-instance";
		return Arrays.asList(((ResponseEntity<ProcessInstance[]>) doPost(url, data, ProcessInstance[].class, user)).getBody());
	}

	@Override
	public ProcessDiagram fetchDiagram(String id, CIBUser user) {
		String url = getEngineRestUrl(user) + "/process-definition/" + id + "/xml";
		return ((ResponseEntity<ProcessDiagram>) doGet(url, ProcessDiagram.class, user, false)).getBody();
	}

	@Override
	public StartForm fetchStartForm(String processDefinitionId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/process-definition/" + processDefinitionId + "/startForm";
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
		String url = getEngineRestUrl(user) + "/process-instance/" + processInstanceId + "/suspended";
		doPut(url, "{ \"suspended\": " + suspend + " }", user);
	}

	@Override
	public void deleteProcessInstance(String processInstanceId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/process-instance/" + processInstanceId;
		doDelete(url, user);
	}

	@Override
	public void suspendProcessDefinition(String processDefinitionId, Boolean suspend, Boolean includeProcessInstances, String executionDate, CIBUser user) {
		String url = getEngineRestUrl(user) + "/process-definition/" + processDefinitionId + "/suspended";
		doPut(url, "{ "
				+ "\"suspended\": " + suspend + ","
				+ "\"includeProcessInstances\": " + includeProcessInstances + ","
				+ "\"executionDate\": " + executionDate 
				+ " }", user);
	}	

	@Override
	public ProcessStart startProcess(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		String url = getEngineRestUrl(user) + "/process-definition/key/" + processDefinitionKey;
		url += (tenantId != null ? ("/tenant-id/" + tenantId) : "") + "/start";
		return ((ResponseEntity<ProcessStart>) doPost(url, data, ProcessStart.class, user)).getBody();
	}

	@Override
	public ProcessStart submitForm(String processDefinitionKey, String tenantId, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		// Used by Webdesk
		String url = getEngineRestUrl(user) + "/process-definition/key/" + processDefinitionKey;
		url += (tenantId != null ? ("/tenant-id/" + tenantId) : "") + "/submit-form";
		return ((ResponseEntity<ProcessStart>) doPost(url, data, ProcessStart.class, user)).getBody();
	}

	@Override
	public ProcessStart submitForm(String processDefinitionKey, String formResult, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		String url = getEngineRestUrl(user) + "/process-definition/" + processDefinitionKey + "/submit-form";	
		return doPost(url, formResult, ProcessStart.class, user).getBody();
	}

	@Override
	public Collection<ProcessStatistics> findProcessStatistics(String processId, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException {
		String url = getEngineRestUrl(user) + "/process-definition/" + processId + "/statistics?failedJobs=true&incidents=true";
		return Arrays.asList(((ResponseEntity<ProcessStatistics[]>) doGet(url, ProcessStatistics[].class, user, false)).getBody());
	}

  @Override
  public Collection<ProcessStatistics> getProcessStatistics(Map<String, Object> queryParams, CIBUser user) {
    String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/process-definition/statistics", queryParams);
    return Arrays.asList(((ResponseEntity<ProcessStatistics[]>) doGet(url, ProcessStatistics[].class, user, true)).getBody());
  }

	@Override
	public HistoryProcessInstance findHistoryProcessInstanceHistory(String processInstanceId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/history/process-instance/" + processInstanceId;
		return ((ResponseEntity<HistoryProcessInstance>) doGet(url, HistoryProcessInstance.class, user, false)).getBody();
	}

	@Override
	public Collection<HistoryProcessInstance> findProcessesInstancesHistory(Map<String, Object> data,
			Optional<Integer> firstResult, Optional<Integer> maxResults, CIBUser user) {
		Map<String, Object> queryParams = new HashMap<String, Object>();
		if (firstResult.isPresent()) queryParams.put("firstResult", firstResult.get());
		if (maxResults.isPresent()) queryParams.put("maxResults", maxResults.get());
		String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/history/process-instance", queryParams);
		Collection<HistoryProcessInstance> processes = Arrays.asList(((ResponseEntity<HistoryProcessInstance[]>) doPost(url, data, HistoryProcessInstance[].class, user)).getBody());

		// Check if caller wants incident handling
		Boolean fetchIncidents = (Boolean) data.get("fetchIncidents");
		if (fetchIncidents != null && fetchIncidents) {
			String processDefinitionId = (String) data.get("processDefinitionId");
			if (processDefinitionId != null) {
				@SuppressWarnings("unchecked")
				List<String> activityIdIn = (List<String>) data.get("activeActivityIdIn");

				// Handle case where no processes found with activity filter - fallback to incident-based search
				if ((processes == null || processes.isEmpty()) && activityIdIn != null && !activityIdIn.isEmpty()) {
					String activityId = activityIdIn.get(0);
					Collection<Incident> incidents = incidentProvider.fetchIncidentsByInstanceAndActivityId(processDefinitionId, activityId, user);

					if (incidents != null && !incidents.isEmpty()) {
						Map<String, List<Incident>> incidentsByProcessInstance = incidents.stream()
							.collect(Collectors.groupingBy(Incident::getProcessInstanceId));

						Set<String> processInstanceIds = incidentsByProcessInstance.keySet();

						// Create new query for process instances with incidents
						Map<String, Object> dataIdIn = new HashMap<>(data);
						dataIdIn.put("processInstanceIdIn", processInstanceIds);
						dataIdIn.remove("activeActivityIdIn"); // Remove activity filter for fallback search

						processes = Arrays.asList(
							((ResponseEntity<HistoryProcessInstance[]>) doPost(url, dataIdIn, HistoryProcessInstance[].class, user)).getBody()
						);

						// Associate incidents with process instances
						processes.forEach(p -> p.setIncidents(incidentsByProcessInstance.getOrDefault(p.getId(), Collections.emptyList())));
					}
				} else if (processes != null) {
					// For regular queries, fetch incidents for all returned processes
					processes.forEach(p -> {
						p.setIncidents(incidentProvider.findIncidentByInstanceId(p.getId(), user));
					});
				}
			}
		}

		return processes;
	}

	@Override
	public Collection<HistoryProcessInstance> findProcessesInstancesHistory(String key, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, CIBUser user) {
		String url = getEngineRestUrl(user) + "/history/process-instance?processDefinitionKey=" + key;
		if (active.isPresent()) {
			url += (active.get()) ? "&unfinished=true" : "&finished=true";
		}
		url += "&firstResult=" + firstResult + "&maxResults=" + maxResults;
		return Arrays.asList(((ResponseEntity<HistoryProcessInstance[]>) doGet(url, HistoryProcessInstance[].class, user, false)).getBody());	
	}

	@Override
	public Collection<HistoryProcessInstance> findProcessesInstancesHistoryById(String id, Optional<String> activityId, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, String text, CIBUser user) {
		String url = getEngineRestUrl(user) + "/history/process-instance?processDefinitionId=" + id;
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
		} else if (processes != null) {
			processes.forEach(p -> {
				p.setIncidents(incidentProvider.findIncidentByInstanceId(p.getId(), user));
			});
		}

		return processes;
	}

	@Override
	public Long countProcessesInstancesHistory(Map<String, Object> filters, CIBUser user) {
		String url = getEngineRestUrl(user) + "/history/process-instance/count";
		JsonNode body = ((ResponseEntity<JsonNode>) doPost(url, filters, JsonNode.class, user)).getBody();
		if (body == null) {
			throw new NullPointerException();
		}
		return body.get("count").asLong();
	}

	@Override
	public ProcessInstance findProcessInstance(String processInstanceId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/process-instance/" + processInstanceId;
		return ((ResponseEntity<ProcessInstance>) doGet(url, ProcessInstance.class, user, false)).getBody();
	}

	public Variable fetchProcessInstanceVariableImpl(String processInstanceId, String variableName, boolean deserializeValue, CIBUser user) throws SystemException  {
		String url = getEngineRestUrl(user) + "/process-instance/" + processInstanceId + "/variables/" + variableName;
		url += "?deserializeValue=" + deserializeValue;
		return ((ResponseEntity<Variable>) doGet(url, Variable.class, null, false)).getBody();
	}

	@Override
	public Variable fetchProcessInstanceVariable(String processInstanceId, String variableName, boolean deserializeValue, CIBUser user) throws SystemException  {
		Variable variableSerialized = fetchProcessInstanceVariableImpl(processInstanceId, variableName, false, user);
		Variable variableDeserialized = fetchProcessInstanceVariableImpl(processInstanceId, variableName, true, user);

		if (deserializeValue) {
			variableDeserialized.setValueSerialized(variableSerialized.getValue());
			variableDeserialized.setValueDeserialized(variableDeserialized.getValue());
			return variableDeserialized;
		}
		else {
			variableSerialized.setValueSerialized(variableSerialized.getValue());
			variableSerialized.setValueDeserialized(variableDeserialized.getValue());
			return variableSerialized;
		}
	}
	
	@Override
	public Collection<Process> findCalledProcessDefinitions(String processDefinitionId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/process-definition/" + processDefinitionId + "/static-called-process-definitions";

		return Arrays.asList(((ResponseEntity<Process[]>) doGet(url, Process[].class, user, false)).getBody());
	}

	@Override
	public ResponseEntity<byte[]> getDeployedStartForm(String processDefinitionId, CIBUser user) {
		String url = getEngineRestUrl(user) + "/process-definition/" + processDefinitionId + "/deployed-start-form";
		return doGetWithHeader(url, byte[].class, user, true, MediaType.APPLICATION_OCTET_STREAM);
	}

	@Override
	public ResponseEntity<String> getRenderedForm(String processDefinitionId, Map<String, Object> params, CIBUser user) {
		String url = getEngineRestUrl(user) + "/process-definition/" + processDefinitionId + "/rendered-form";
		try {
			// Build query parameters from the params map
			StringBuilder queryParams = new StringBuilder();
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				if (queryParams.length() > 0) {
					queryParams.append("&");
				}
				queryParams.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()))
					.append("=")
					.append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8.toString()));
			}
			
			if (queryParams.length() > 0) {
				url += "?" + queryParams.toString();
			}
			
			return doGetWithHeader(url, String.class, user, true, MediaType.ALL);
		} catch (UnsupportedEncodingException e) {
			throw new SystemException("Error encoding URL parameters for rendered form request", e);
		}
	}

	@Override
	public void updateHistoryTimeToLive(String id, Map<String, Object> data, CIBUser user) {
		try {
			String url = getEngineRestUrl(user) + "/process-definition/" + id + "/history-time-to-live";
			doPut(url, data, user);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	@Override
	public void deleteProcessInstanceFromHistory(String id, CIBUser user) {
		String url = getEngineRestUrl(user) + "/history/process-instance/" + id;
		doDelete(url, user);
	}

	@Override
	public void deleteProcessDefinition(String id, Optional<Boolean> cascade, CIBUser user) {
		boolean cascadeVal = cascade.orElse(true);
		String url = getEngineRestUrl(user) + "/process-definition/" + id + "?cascade=" + cascadeVal;
		doDelete(url, user);
	}

	/**
	 * Groups ProcessStatistics by key and tenant ID, consolidating different versions
	 * of the same process definition into a single aggregated statistic.
	 * 
	 * @param processStatistics Collection of ProcessStatistics to group
	 * @return List of grouped ProcessStatistics with aggregated values
	 */
	@Override
	public List<ProcessStatistics> groupProcessStatisticsByKeyAndTenant(Collection<ProcessStatistics> processStatistics) {
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
					.orElse(group.get(0));

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
	@Override
	public Object fetchHistoricActivityStatistics(String id, Map<String, Object> params, CIBUser user) {
	    String url = URLUtils.buildUrlWithParams(getEngineRestUrl(user) + "/history/process-definition/" + id + "/statistics", params);
	    ResponseEntity<Object> response = doGet(url, Object.class, user, true);
	    return response.getBody();
	}
}
