package org.cibseven.providers;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.Data;
import org.cibseven.auth.CIBUser;
import org.cibseven.exception.ExpressionEvaluationException;
import org.cibseven.exception.SystemException;
import org.cibseven.exception.UnsupportedTypeException;
import org.cibseven.rest.model.Process;
import org.cibseven.rest.model.ProcessDiagram;
import org.cibseven.rest.model.ProcessInstance;
import org.cibseven.rest.model.ProcessStart;
import org.cibseven.rest.model.ProcessStatistics;
import org.cibseven.rest.model.StartForm;
import org.cibseven.rest.model.Variable;
import org.springframework.http.ResponseEntity;

public interface IProcessProvider {
	
	public Collection<Process> findProcesses(CIBUser user);
	public Collection<Process> findProcessesWithInfo(CIBUser user);
	public Collection<Process> findProcessesWithFilters(String filters, CIBUser user);
	public Process findProcessByDefinitionKey(String key, CIBUser user);
	public Collection<Process> findProcessVersionsByDefinitionKey(String key, Optional<Boolean> lazyLoad, CIBUser user);
	public Process findProcessById(String id, Optional<Boolean> extraInfo, CIBUser user) throws SystemException;
	public Collection<ProcessInstance> findProcessesInstances(String key, CIBUser user);
	public Collection<ProcessInstance> findCurrentProcessesInstances(Map<String, Object> data, CIBUser user);
	public ProcessDiagram fetchDiagram(String id, CIBUser user);
	public StartForm fetchStartForm(String processDefinitionId, CIBUser user);
	public Data downloadBpmn(String id, String fileName, CIBUser user);
	public void suspendProcessInstance(String processInstanceId, Boolean suspend, CIBUser user);
	public void deleteProcessInstance(String processInstanceId, CIBUser user);
	public void suspendProcessDefinition(String processDefinitionId, Boolean suspend, Boolean includeProcessInstances, String executionDate, CIBUser user);
	public ProcessStart startProcess(String processDefinitionKey, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException;
	public ProcessStart submitForm(String processDefinitionKey, Map<String, Object> data, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException;
	public Collection<ProcessStatistics> findProcessStatistics(String processId, CIBUser user) throws SystemException, UnsupportedTypeException, ExpressionEvaluationException;
	public ProcessInstance findHistoryProcessInstanceHistory(String processInstanceId, CIBUser user);
	public Collection<ProcessInstance> findProcessesInstancesHistory(String key, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, CIBUser user);
	public Collection<ProcessInstance> findProcessesInstancesHistoryById(String id, Optional<String> activityId, Optional<Boolean> active, 
			Integer firstResult, Integer maxResults, String text, CIBUser user);
	public ProcessInstance findProcessInstance(String processInstanceId, CIBUser user);
	public Variable fetchProcessInstanceVariable(String processInstanceId, String variableName, String deserializeValue, CIBUser user) throws SystemException;
	public Collection<Process> findCalledProcessDefinitions(String processDefinitionId, CIBUser user);
	public ResponseEntity<byte[]> getDeployedStartForm(String processDefinitionId, CIBUser user);
	public void updateHistoryTimeToLive(String id, Map<String, Object> data, CIBUser user);
	public void deleteProcessInstanceFromHistory(String id, CIBUser user);
	public void deleteProcessDefinition(String id, Optional<Boolean> cascade, CIBUser user);
	
}
