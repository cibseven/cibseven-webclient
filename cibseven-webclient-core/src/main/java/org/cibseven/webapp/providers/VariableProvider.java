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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.cibseven.webapp.NamedByteArrayDataSource;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.UnexpectedTypeException;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.activation.DataSource;
import lombok.extern.slf4j.Slf4j;

// THIS CLASS IS DIFFERENT IN CIB FLOW.

@Slf4j
@Component
public class VariableProvider extends SevenProviderBase implements IVariableProvider {

	// from org.camunda.bpm.engine.variable.Variables SerializationDataFormats
	private static final String SERIALIZATION_DATA_FORMAT_JSON = "application/json";

	@Override
	public void modifyVariableByExecutionId(String executionId, Map<String, Object> data, CIBUser user) throws SystemException {
		String url = getEngineRestUrl() + "/execution/" + executionId + "/localVariables/";
		doPost(url, data, null, user);
	}

	@Override
	public void modifyVariableDataByExecutionId(String executionId, String variableName, MultipartFile file, CIBUser user) throws SystemException {
		String url = getEngineRestUrl() + "/execution/" + executionId + "/localVariables/" + variableName + "/data";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		if (user != null) headers.add("Authorization", user.getAuthToken());
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		try {
			body.add("data", file.getResource());
			body.add("valueType", "File");
			HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

			customRestTemplate.exchange(builder.build().toUri(), HttpMethod.POST, request, String.class);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	@Override
	public Collection<Variable> fetchProcessInstanceVariables(String processInstanceId, Map<String, Object> data, CIBUser user) throws SystemException {

		UriComponentsBuilder uriBuilder = UriComponentsBuilder
			.fromUriString(getEngineRestUrl())
			.path("/variable-instance");

		if (data != null) {
			data.forEach((key, value) -> {
				if (value != null) {
					uriBuilder.queryParam(key, value);
				}
			});
		}
		uriBuilder.queryParam("processInstanceIdIn", processInstanceId);

		String url = uriBuilder.build().toUriString();
		return Arrays.asList(((ResponseEntity<VariableHistory[]>) doGet(url, VariableHistory[].class, user, false)).getBody());
	}

	@Override
	public ResponseEntity<byte[]> fetchVariableDataByExecutionId(String executionId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException  {
		String url = getEngineRestUrl() + "/execution/" + executionId + "/localVariables/" + variableName + "/data";

		try {
		    // Use doGetWithHeader which creates a new RestTemplate instance for thread safety
		    return doGetWithHeader(url, byte[].class, user, true, MediaType.APPLICATION_OCTET_STREAM);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	@Override
	public Collection<VariableHistory> fetchProcessInstanceVariablesHistory(String processInstanceId, Map<String, Object> data, CIBUser user) throws SystemException {

		UriComponentsBuilder uriBuilder = UriComponentsBuilder
			.fromUriString(getEngineRestUrl())
			.path("/history/variable-instance");

		if (data != null) {
			data.forEach((key, value) -> {
				if (value != null) {
					uriBuilder.queryParam(key, value);
				}
			});
		}
		uriBuilder.queryParam("processInstanceIdIn", processInstanceId);

		String url = uriBuilder.build().toUriString();
		return Arrays.asList(((ResponseEntity<VariableHistory[]>) doGet(url, VariableHistory[].class, user, false)).getBody());
	}

	@Override
	public Collection<VariableHistory> fetchActivityVariablesHistory(String activityInstanceId, CIBUser user) {
		String url = getEngineRestUrl() + "/history/variable-instance?activityInstanceIdIn=" + activityInstanceId;
		return Arrays.asList(((ResponseEntity<VariableHistory[]>) doGet(url, VariableHistory[].class, user, false)).getBody());
	}

	@Override
	public Collection<VariableHistory> fetchActivityVariables(String activityInstanceId, CIBUser user) {
		String url = getEngineRestUrl() + "/variable-instance?activityInstanceIdIn=" + activityInstanceId;
		return Arrays.asList(((ResponseEntity<VariableHistory[]>) doGet(url, VariableHistory[].class, user, false)).getBody());
	}	

	@Override
	public ResponseEntity<byte[]> fetchHistoryVariableDataById(String id, CIBUser user) throws NoObjectFoundException, SystemException  {
		String url = getEngineRestUrl() + "/history/variable-instance/" + id + "/data";

		try {
		    // Use doGetWithHeader which creates a new RestTemplate instance for thread safety
		    return doGetWithHeader(url, byte[].class, user, true, MediaType.APPLICATION_OCTET_STREAM);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	@Override
	public Variable fetchVariable(String taskId, String variableName, 
			Optional<Boolean> deserializeValue, CIBUser user) throws NoObjectFoundException, SystemException {		
		String url = getEngineRestUrl() + "/task/" + taskId + "/variables/" + variableName;
		url += deserializeValue.isPresent() ? "?deserializeValue=" + deserializeValue.get() : "";
		return doGet(url, Variable.class, user, false).getBody();
	}

	@Override
	public void deleteVariable(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, SystemException {		
		String url = getEngineRestUrl() + "/task/" + taskId + "/variables/" + variableName;
		doDelete(url, user);
	}

	@Override
	public Map<String, Variable> fetchFormVariables(String taskId, boolean deserializeValues, CIBUser user) throws NoObjectFoundException, SystemException {
		String url = getEngineRestUrl() + "/task/" + taskId + "/form-variables";
		url += "?deserializeValues=" + deserializeValues;
		return doGet(url, new ParameterizedTypeReference<Map<String, Variable>>() {}, user).getBody();
	}	

	@Override
	public Map<String, Variable> fetchFormVariables(List<String> variableListName, String taskId, CIBUser user) throws NoObjectFoundException, SystemException {
		String url = getEngineRestUrl() + "/task/" + taskId + "/form-variables?variableNames=";

		for(String variable: variableListName) {
			url += variable + ",";
		}

		return doGet(url, new ParameterizedTypeReference<Map<String, Variable>>() {}, user).getBody();
	}

	@Override
	public Map<String, Variable> fetchProcessFormVariables(String key, CIBUser user) throws NoObjectFoundException, SystemException {
		String url = getEngineRestUrl() + "/process-definition/key/" + key + "/form-variables";
		return doGet(url, new ParameterizedTypeReference<Map<String, Variable>>() {}, user).getBody();
	}	

	@Override
	public NamedByteArrayDataSource fetchVariableFileData(String taskId, String variableName, CIBUser user) throws NoObjectFoundException, UnexpectedTypeException, SystemException {		
		String url = getEngineRestUrl() + "/task/" + taskId + "/variables/" + variableName + "/data";
		try {

		    byte[] data = null;
		    String filename = null;
		    String mimeType = null;

		    Variable variable = fetchVariable(taskId, variableName, Optional.of(true), user);
			String objectType = variable.getValueInfo().get("objectTypeName");
			if (objectType != null) {
				try {
					Class<?> clazz =  Class.forName(objectType);

					if (DataSource.class.isAssignableFrom(clazz)) {
						final ObjectMapper mapper = new ObjectMapper();
						@SuppressWarnings("unchecked")
						DataSource ds = mapper.convertValue(variable.getValue(), (Class<? extends DataSource>) clazz);

						return new NamedByteArrayDataSource(ds.getName(), ds.getContentType(),
								IOUtils.toByteArray(ds.getInputStream()));
					}
				} catch (ClassNotFoundException e) {
					log.info("Class " + objectType + " could not be loaded!");
				}
			}

		    filename = variable.getFilename();
		    mimeType = variable.getMimeType();

			ResponseEntity<byte[]> response = doGetWithHeader(url, byte[].class, user, true, MediaType.APPLICATION_OCTET_STREAM);
		    data = response.getBody();

		    return new NamedByteArrayDataSource(filename, mimeType, data);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		} catch (IOException e) {
			throw new SystemException(e);
		}
	}

	@Override
	public ResponseEntity<byte[]> fetchProcessInstanceVariableData(String processInstanceId, String variableName,
			CIBUser user) throws NoObjectFoundException, SystemException {
		String url = getEngineRestUrl() + "/process-instance/" + processInstanceId + "/variables/" + variableName + "/data";

		try {
		    byte[] data = null;
		    Variable variable = fetchVariableByProcessInstanceId(processInstanceId, variableName, user);
			String objectType = variable.getValueInfo().get("objectTypeName");
			if (objectType != null) {
				try {
					Class<?> clazz =  Class.forName(objectType);

					if (DataSource.class.isAssignableFrom(clazz)) {
						final ObjectMapper mapper = new ObjectMapper();
						@SuppressWarnings("unchecked")
						DataSource ds = mapper.convertValue(variable.getValue(), (Class<? extends DataSource>) clazz);

						//return new ResponseEntity<>(IOUtils.toByteArray(ds.getInputStream()), HttpStatus.OK);
						return generateFileResponse(IOUtils.toByteArray(ds.getInputStream()));
					}
				} catch (ClassNotFoundException e) {
					log.info("Class " + objectType + " could not be loaded!");
				}
			}

			ResponseEntity<byte[]> response = doGetWithHeader(url, byte[].class, user, true, MediaType.APPLICATION_OCTET_STREAM);
		    data = response.getBody();
			return generateFileResponse(data); //ResponseEntity<>(data, HttpStatus.OK);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		} catch (IOException e) {
			throw new SystemException(e);
		}
	}

	// TODO: Split it
	@Override
	public ProcessStart submitStartFormVariables(String processDefinitionId, List<Variable> formResult, CIBUser user) throws SystemException {
		//String url = camundaUrl + "/engine-rest/process-definition/key/" + processDefinitionKey + "/submit-form";
		String url = getEngineRestUrl() + "/process-definition/" + processDefinitionId + "/submit-form";
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode variables = mapper.getNodeFactory().objectNode();
		ObjectNode modifications = mapper.getNodeFactory().objectNode();
		try {		
			for (Variable variable: formResult) {
				ObjectNode variablePost = mapper.getNodeFactory().objectNode();
				String val = String.valueOf(variable.getValue());
				if (variable.getType().equals("Boolean")) {
					variablePost.put("value", Boolean.parseBoolean(val));
				} else if (variable.getType().equals("Double")) {
					variablePost.put("value", Double.parseDouble(val));
				}
				else variablePost.put("value", val);

				if(variable.getType().equals("file")) {

					//https://helpdesk.cib.de/browse/BPM4CIB-434
					int lastIndex = variable.getFilename().lastIndexOf(".rtf");
					if ((lastIndex > 0) && ((lastIndex + 4) == variable.getFilename().length())) {
						variable.getValueInfo().put("mimeType", "application/rtf");
					}

				}

				if (variable.getType().equals("Object")) {
					variablePost.set("valueInfo", mapper.valueToTree(variable.getValueInfo()));
					variablePost.put("type", "Object");
					try {
						variablePost.put("value", mapper.writeValueAsString(variable.getValue()));
					} catch (IOException e) {
						SystemException se = new SystemException(e);
						log.info("Exception in submitVariables(...):", se);
						throw se;
					}
				}

				variables.set(variable.getName(), variablePost);
			}

			modifications.set("variables", variables);
			doPost(url, modifications, String.class, user);
		} catch (HttpStatusCodeException e) {
			SystemException se = new SystemException(e.getResponseBodyAsString() + "[VARIABLES] " + variables, e);
			log.info("Exception in submitStartFormVariables(...):", se);
			throw se;
		}
	}

	@Override
	public Variable fetchVariableByProcessInstanceId(String processInstanceId, String variableName, CIBUser user) throws SystemException {
		String url = getEngineRestUrl() + "/process-instance/" + processInstanceId + "/variables/" + variableName + "?deserializeValue=true";
		return doGet(url, Variable.class, user, false).getBody();
	}

	// TODO: Split it
	@Override
	public void saveVariableInProcessInstanceId(String processInstanceId, List<Variable> variables, CIBUser user) throws SystemException {
		String url = getEngineRestUrl() + "/process-instance/" + processInstanceId + "/variables";

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode variablesF = mapper.getNodeFactory().objectNode();
		ObjectNode modifications = mapper.getNodeFactory().objectNode();

		for (Variable variable: variables) {
			ObjectNode variablePost = mapper.getNodeFactory().objectNode();	
			variablePost.put("value", String.valueOf(variable.getValue()));
			variablePost.put("type", variable.getType());
			variablesF.set(variable.getName(), variablePost);
		}

		modifications.set("modifications", variablesF);

		try {
			doPost(url, modifications, String.class, user);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	// TODO: Split it
	@Override
	public void submitVariables(String processInstanceId, List<Variable> formResult, CIBUser user, String processDefinitionId) throws SystemException {
		String url = getEngineRestUrl() + "/process-instance/" + processInstanceId + "/variables";

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode variables = mapper.getNodeFactory().objectNode();
		ObjectNode modifications = mapper.getNodeFactory().objectNode();

		for (Variable variable: formResult) {
			ObjectNode variablePost = mapper.getNodeFactory().objectNode();
			String val = String.valueOf(variable.getValue());
			if (variable.getType().equals("Boolean")) {
				variablePost.put("value", Boolean.parseBoolean(val));
			} else if (variable.getType().equals("Double")) {
				variablePost.put("value", Double.parseDouble(val));
			}
			else variablePost.put("value", val);
			//TODO Changing variables before saving should be done in the task classes

			if (variable.getType().equals("file")) {

				//https://helpdesk.cib.de/browse/BPM4CIB-434
				int lastIndex = variable.getFilename().lastIndexOf(".rtf");
				if ((lastIndex > 0) && ((lastIndex + 4) == variable.getFilename().length())) {
					variable.getValueInfo().put("mimeType", "application/rtf");
				}
			}

			if (variable.getType().equals("json")) {
				variablePost.set("valueInfo", mapper.valueToTree(variable.getValueInfo()));
				variablePost.put("type", "json");
				try {
					variablePost.put("value", mapper.writeValueAsString(variable.getValue()));
				} catch (IOException e) {
					SystemException se = new SystemException(e);
					log.info("Exception in submitVariables(...):", se);
					throw se;
				}
			}

			if (variable.getType().equals("Object")) {
				variablePost.set("valueInfo", mapper.valueToTree(variable.getValueInfo()));
				variablePost.put("type", "Object");
				try {
					variablePost.put("value", mapper.writeValueAsString(variable.getValue()));
				} catch (IOException e) {
					SystemException se = new SystemException(e);
					log.info("Exception in submitVariables(...):", se);
					throw se;
				}
			}

			variables.set(variable.getName(), variablePost);
		}

		modifications.set("modifications", variables);

		try {
			doPost(url, modifications, String.class, user);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	@Override
	public Map<String, Variable> fetchProcessFormVariablesById(String id, CIBUser user) throws SystemException {
		String url = getEngineRestUrl() + "/process-definition/" + id + "/form-variables";
		return doGet(url, new ParameterizedTypeReference<Map<String, Variable>>() {}, user).getBody();
	}

	@Override
	public void putLocalExecutionVariable(String executionId, String varName, Map<String, Object> data, CIBUser user) {
		String url = getEngineRestUrl() + "/execution/" + executionId + "/localVariables/" + varName;
		doPut(url, data, user);
	}

	protected ResponseEntity<byte[]> generateFileResponse(byte[] content) throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl(CacheControl.noCache().getHeaderValue());
		headers.add("Content-Type", "application/octet-stream");
		ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(content, headers, HttpStatus.OK);
		return responseEntity;
	}


}
