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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.cibseven.webapp.NamedByteArrayDataSource;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.UnexpectedTypeException;
import org.cibseven.webapp.exception.UnsupportedTypeException;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
	public void modifyVariableDataByExecutionId(String executionId, String variableName, MultipartFile data, String valueType, CIBUser user) throws SystemException {
		String url = getEngineRestUrl() + "/execution/" + executionId + "/localVariables/" + variableName + "/data";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		if (user != null) headers.add("Authorization", user.getAuthToken());
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		try {
      
      if (valueType.equalsIgnoreCase("File") || valueType.equalsIgnoreCase("Bytes")) {
        // Handle binary/file data
        body.add("data", data.getResource());
        body.add("valueType", valueType);
      } else {
        // Handle JSON/serialized data
        String jsonContent = new String(data.getBytes());
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> jsonEntity = new HttpEntity<>(jsonContent, jsonHeaders);
        
        body.add("data", jsonEntity);
        body.add("type", valueType);
        body.add("valueType", SERIALIZATION_DATA_FORMAT_JSON);
      }
      
			HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
			RestTemplate rest = new RestTemplate();

			rest.exchange(builder.build().toUri(), HttpMethod.POST, request, String.class);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		} catch (IOException e) { // from data.getBytes()
      throw new UnsupportedTypeException(e);
    }
	}

	private void mergeVariablesValues(
		Collection<Variable> variablesDeserialized,
		Collection<Variable> variablesSerialized,
		boolean deserializeValues) {

		if (variablesDeserialized == null) {
			return;
		}

		if (variablesSerialized == null) {
			return;
		}

		Collection<Variable> variables = (deserializeValues) ? variablesDeserialized : variablesSerialized;
		variables.forEach(variable -> {
			String name = variable.getName();

			Variable variableSerialized = (!deserializeValues) ? variable : variablesSerialized.stream()
				.filter(v -> v.getName().equals(name))
				.findFirst()
				.orElse(null);
			if (variableSerialized != null) {
				variable.setValueSerialized(variableSerialized.getValue());
			}

			Variable variableDeserialized = (deserializeValues) ? variable : variablesDeserialized.stream()
				.filter(v -> v.getName().equals(name))
				.findFirst()
				.orElse(null);
			if (variableDeserialized != null) {
				variable.setValueDeserialized(variableDeserialized.getValue());
			}
		});
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

		final boolean deserializeValues = data != null
			&& data.containsKey("deserializeValues")
			&& (Boolean) data.get("deserializeValues");

		uriBuilder.replaceQueryParam("deserializeValues", "true");
		String urlDeserialized = uriBuilder.build().toUriString();
		Collection<Variable> variablesDeserialized = Arrays.asList(((ResponseEntity<VariableHistory[]>) doGet(urlDeserialized, VariableHistory[].class, user, false)).getBody());
		if (variablesDeserialized == null) {
			return Collections.emptyList();
		}

		uriBuilder.replaceQueryParam("deserializeValues", "false");
		String urlSerialized = uriBuilder.build().toUriString();
		Collection<Variable> variablesSerialized = Arrays.asList(((ResponseEntity<VariableHistory[]>) doGet(urlSerialized, VariableHistory[].class, user, false)).getBody());
		if (variablesSerialized == null) {
			return Collections.emptyList();
		}

		mergeVariablesValues(
			variablesDeserialized,
			variablesSerialized,
			deserializeValues);

		Collection<Variable> variables = (deserializeValues) ? variablesDeserialized : variablesSerialized;
		return variables;
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

		final boolean deserializeValues = data != null
			&& data.containsKey("deserializeValues")
			&& (Boolean) data.get("deserializeValues");

		uriBuilder.replaceQueryParam("deserializeValues", "true");
		String urlDeserialized = uriBuilder.build().toUriString();
		Collection<VariableHistory> variablesDeserialized = Arrays.asList(((ResponseEntity<VariableHistory[]>) doGet(urlDeserialized, VariableHistory[].class, user, false)).getBody());
		if (variablesDeserialized == null) {
			return Collections.emptyList();
		}

		uriBuilder.replaceQueryParam("deserializeValues", "false");
		String urlSerialized = uriBuilder.build().toUriString();
		Collection<VariableHistory> variablesSerialized = Arrays.asList(((ResponseEntity<VariableHistory[]>) doGet(urlSerialized, VariableHistory[].class, user, false)).getBody());
		if (variablesSerialized == null) {
			return Collections.emptyList();
		}

		// Get list of variables and merge them
		final ArrayList<Variable> variablesDeserializedTyped = new ArrayList<>();
		if (variablesDeserialized.size() > 0) {
			variablesDeserializedTyped.addAll(variablesDeserialized);
		}

		final ArrayList<Variable> variablesSerializedTyped = new ArrayList<>();
		if (variablesSerialized.size() > 0) {
			variablesSerializedTyped.addAll(variablesSerialized);
		}

		mergeVariablesValues(
			variablesDeserializedTyped,
			variablesSerializedTyped,
			deserializeValues);

		Collection<VariableHistory> variables = (deserializeValues) ? variablesDeserialized : variablesSerialized;
		return variables;
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

	public Variable fetchVariableImpl(String taskId, String variableName, 
			boolean deserializeValue, CIBUser user) throws NoObjectFoundException, SystemException {		
		String url = getEngineRestUrl() + "/task/" + taskId + "/variables/" + variableName
			+ "?deserializeValue=" + deserializeValue;
		return doGet(url, Variable.class, user, false).getBody();
	}

	@Override
	public Variable fetchVariable(String taskId, String variableName, 
			boolean deserializeValue, CIBUser user) throws NoObjectFoundException, SystemException {		
		Variable variableSerialized = fetchVariableImpl(taskId, variableName, false, user);
		Variable variableDeserialized = fetchVariableImpl(taskId, variableName, true, user);

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
		    
		    Variable variable = fetchVariable(taskId, variableName, true, user);
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

	/**
	 * Uploads file data for a specific variable of a task.
	 *
	 * @param taskId the ID of the task to which the variable belongs
	 * @param variableName the name of the variable to upload data for
	 * @param data the file data to upload
	 * @param valueType the type of the variable value
	 * @param user the user performing the upload operation
	 * @throws NoObjectFoundException if the task or variable is not found
	 * @throws SystemException if an internal error occurs during upload
	 */
	@Override
	public void uploadVariableFileData(String taskId, String variableName, MultipartFile data, String valueType, CIBUser user) throws NoObjectFoundException, SystemException {
		String url = getEngineRestUrl() + "/task/" + taskId + "/variables/" + variableName + "/data";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		if (user != null) headers.add("Authorization", user.getAuthToken());
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		try {
			body.add("data", data.getResource());
			body.add("valueType", valueType);
			HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

			customRestTemplate.exchange(builder.build().toUri(), HttpMethod.POST, request, String.class);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
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

    /**
     * Uploads file data for a variable of a specific process instance.
     *
     * @param processInstanceId the ID of the process instance
     * @param variableName the name of the variable to which the file data will be uploaded
     * @param data the file data to upload
     * @param valueType the type of the value being uploaded
     * @param user the user performing the upload operation
     * @throws NoObjectFoundException if the process instance or variable is not found
     * @throws SystemException if a system error occurs during the upload
     */
    @Override
    public void uploadProcessInstanceVariableFileData(String processInstanceId, String variableName, MultipartFile data, String valueType, CIBUser user) throws NoObjectFoundException, SystemException {
        String url = getEngineRestUrl() + "/process-instance/" + processInstanceId + "/variables/" + variableName + "/data";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		if (user != null) headers.add("Authorization", user.getAuthToken());
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		try {
			body.add("data", data.getResource());
			body.add("valueType", valueType);
			HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

			customRestTemplate.exchange(builder.build().toUri(), HttpMethod.POST, request, String.class);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
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
                } else if (variable.getType().equals("Integer")) {
                    variablePost.put("value", Integer.parseInt(val));
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
		
				if (variable.getType().equals("File")) {
					variablePost.set("valueInfo", mapper.valueToTree(variable.getValueInfo()));
					variablePost.put("type", "File");
				}

				variables.set(variable.getName(), variablePost);
			}

			modifications.set("variables", variables);
			return doPost(url, modifications, ProcessStart.class, user).getBody();
		} catch (HttpStatusCodeException e) {
			SystemException se = new SystemException(e.getResponseBodyAsString() + "[VARIABLES] " + variables, e);
			log.info("Exception in submitStartFormVariables(...):", se);
			throw se;
		}
	}

	private Variable fetchVariableByProcessInstanceIdImpl(String processInstanceId, String variableName, boolean deserializeValue, CIBUser user) throws SystemException {
		String url = getEngineRestUrl() + "/process-instance/" + processInstanceId + "/variables/" + variableName + "?deserializeValue=" + deserializeValue;
		return doGet(url, Variable.class, user, false).getBody();
	}

	@Override
	public Variable fetchVariableByProcessInstanceId(String processInstanceId, String variableName, CIBUser user) throws SystemException {
		Variable variableSerialized = fetchVariableByProcessInstanceIdImpl(processInstanceId, variableName, false, user);
		Variable variableDeserialized = fetchVariableByProcessInstanceIdImpl(processInstanceId, variableName, true, user);

		variableDeserialized.setValueSerialized(variableSerialized.getValue());
		variableDeserialized.setValueDeserialized(variableDeserialized.getValue());
		return variableDeserialized;
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
			} else if (variable.getType().equals("Integer")) {
			  	variablePost.put("value", Integer.parseInt(val));
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
