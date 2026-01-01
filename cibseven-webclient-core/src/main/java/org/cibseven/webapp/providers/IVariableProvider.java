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
import org.cibseven.webapp.NamedByteArrayDataSource;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.UnexpectedTypeException;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface IVariableProvider {

	public void modifyVariableByExecutionId(String executionId, Map<String, Object> data, CIBUser user) throws SystemException;
	public void modifyVariableDataByExecutionId(String executionId, String variableName, MultipartFile data, String valueType, CIBUser user) throws SystemException;
	public Collection<Variable> fetchProcessInstanceVariables(String processInstanceId, Map<String, Object> data, CIBUser user) throws NoObjectFoundException, SystemException;
	public ResponseEntity<byte[]> fetchVariableDataByExecutionId(String executionId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException;
	public Collection<VariableHistory> fetchProcessInstanceVariablesHistory(String processInstanceId, Map<String, Object> data, CIBUser user) throws SystemException;
	public Collection<VariableHistory> fetchActivityVariablesHistory(String activityInstanceId, CIBUser user);
	public Collection<VariableHistory> fetchActivityVariables(String activityInstanceId, CIBUser user);
	public ResponseEntity<byte[]> fetchHistoryVariableDataById(String id, CIBUser user) throws NoObjectFoundException, SystemException;
	public Variable fetchVariable(String taskId, String variableName, 
			boolean deserializeValue, CIBUser user) throws NoObjectFoundException, SystemException;
	public void deleteVariable(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException;
	public Map<String, Variable> fetchFormVariables(String taskId, boolean deserializeValues, CIBUser user) throws NoObjectFoundException, SystemException;
	public Map<String, Variable> fetchFormVariables(List<String> variableListName, String taskId, boolean deserializeValues, CIBUser user) throws NoObjectFoundException, SystemException;
	public Map<String, Variable> fetchProcessFormVariables(String key, boolean deserializeValues, CIBUser user) throws NoObjectFoundException, SystemException;
	public Map<String, Variable> fetchProcessFormVariables(List<String> variableListName, String key, boolean deserializeValues, CIBUser user) throws NoObjectFoundException, SystemException;
	public NamedByteArrayDataSource fetchVariableFileData(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, UnexpectedTypeException, SystemException;
	public void uploadVariableFileData(String taskId, String variableName, MultipartFile data, String valueType, CIBUser user) throws NoObjectFoundException, SystemException;
	public ResponseEntity<byte[]> fetchProcessInstanceVariableData(String processInstanceId, String variableName,
			CIBUser user) throws NoObjectFoundException, SystemException;
	public void uploadProcessInstanceVariableFileData(String processInstanceId, String variableName, MultipartFile data, String valueType, CIBUser user) throws NoObjectFoundException, SystemException;
	public ProcessStart submitStartFormVariables(String processDefinitionId, List<Variable> formResult, CIBUser user) throws SystemException;
	public Variable fetchVariableByProcessInstanceId(String processInstanceId, String variableName, CIBUser user) throws SystemException;
	public void saveVariableInProcessInstanceId(String processInstanceId, List<Variable> variables, CIBUser user) throws SystemException;
	public void submitVariables(String processInstanceId, List<Variable> formResult, CIBUser user, String processDefinitionId) throws SystemException;
	public Map<String, Variable> fetchProcessFormVariablesById(String id, CIBUser user) throws SystemException;
	public void putLocalExecutionVariable(String executionId, String varName, Map<String, Object> data, CIBUser user);
	
}