package org.cibseven.providers;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cibseven.NamedByteArrayDataSource;
import org.cibseven.auth.CIBUser;
import org.cibseven.exception.NoObjectFoundException;
import org.cibseven.exception.SystemException;
import org.cibseven.exception.UnexpectedTypeException;
import org.cibseven.rest.model.ProcessStart;
import org.cibseven.rest.model.Variable;
import org.cibseven.rest.model.VariableHistory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface IVariableProvider {

	public void modifyVariableByExecutionId(String executionId, Map<String, Object> data, CIBUser user) throws SystemException;
	public void modifyVariableDataByExecutionId(String executionId, String variableName, MultipartFile file, CIBUser user) throws SystemException;
	public Collection<Variable> fetchProcessInstanceVariables(String processInstanceId, CIBUser user, Optional<Boolean> deserializeValue) throws SystemException;
	public ResponseEntity<byte[]> fetchVariableDataByExecutionId(String executionId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException;
	public Collection<VariableHistory> fetchProcessInstanceVariablesHistory(String processInstanceId, CIBUser user, Optional<Boolean> deserializeValue);
	public Collection<VariableHistory> fetchActivityVariablesHistory(String activityInstanceId, CIBUser user);
	public Collection<VariableHistory> fetchActivityVariables(String activityInstanceId, CIBUser user);
	public ResponseEntity<byte[]> fetchHistoryVariableDataById(String id, CIBUser user) throws NoObjectFoundException, SystemException;
	public Variable fetchVariable(String taskId, String variableName, 
			Optional<Boolean> deserializeValue, CIBUser user) throws NoObjectFoundException, SystemException;
	public void deleteVariable(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException;
	public Map<String, Variable> fetchFormVariables(String taskId, boolean deserializeValues, CIBUser user) throws NoObjectFoundException, SystemException;
	public Map<String, Variable> fetchFormVariables(List<String> variableListName, String taskId, CIBUser user) throws NoObjectFoundException, SystemException;
	public Map<String, Variable> fetchProcessFormVariables(String key, CIBUser user) throws NoObjectFoundException, SystemException;
	public NamedByteArrayDataSource fetchVariableFileData(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, UnexpectedTypeException, SystemException;
	public ResponseEntity<byte[]> fetchProcessInstanceVariableData(String processInstanceId, String variableName,
			CIBUser user) throws NoObjectFoundException, SystemException;
	public ProcessStart submitStartFormVariables(String processDefinitionId, List<Variable> formResult, CIBUser user) throws SystemException;
	public Variable fetchVariableByProcessInstanceId(String processInstanceId, String variableName, CIBUser user) throws SystemException;
	public void saveVariableInProcessInstanceId(String processInstanceId, List<Variable> variables, CIBUser user) throws SystemException;
	public void submitVariables(String processInstanceId, List<Variable> formResult, CIBUser user, String processDefinitionId) throws SystemException;
	public Map<String, Variable> fetchProcessFormVariablesById(String id, CIBUser user) throws SystemException;
	
}