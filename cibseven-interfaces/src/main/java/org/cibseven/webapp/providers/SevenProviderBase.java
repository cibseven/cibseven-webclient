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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.exception.BatchOperationException;
import org.cibseven.webapp.exception.DmnTransformationException;
import org.cibseven.webapp.exception.ExistingGroupRequestException;
import org.cibseven.webapp.exception.ExistingUserRequestException;
import org.cibseven.webapp.exception.ExpressionEvaluationException;
import org.cibseven.webapp.exception.InvalidAttributeValueException;
import org.cibseven.webapp.exception.InvalidUserIdException;
import org.cibseven.webapp.exception.InvalidValueHistoryTimeToLive;
import org.cibseven.webapp.exception.MissingVariableException;
import org.cibseven.webapp.exception.NoObjectFoundException;
import org.cibseven.webapp.exception.NoRessourcesFoundException;
import org.cibseven.webapp.exception.OptimisticLockingException;
import org.cibseven.webapp.exception.PasswordPolicyException;
import org.cibseven.webapp.exception.SubmitDeniedException;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.exception.UnsupportedTypeException;
import org.cibseven.webapp.exception.VariableModificationException;
import org.cibseven.webapp.exception.WrongDeploymenIdException;
import org.cibseven.webapp.rest.model.Authorization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cibseven.webapp.rest.CustomRestTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SevenProviderBase {

  protected static final String USER_ID_HEADER = "Context-User-ID";

	@Value("${cibseven.webclient.custom.spring.jackson.parser.max-size:20000000}") int jacksonParserMaxSize;
	@Value("${cibseven.webclient.engineRest.url:./}") protected String cibsevenUrl;
	@Value("${cibseven.webclient.engineRest.path:/engine-rest}") protected String engineRestPath;

	@Autowired
	protected CustomRestTemplate customRestTemplate;

	@Autowired 
	@Lazy
	protected BaseUserProvider<? extends StandardLogin> baseUserProvider;

	/**
	 * Constructs the full engine REST base URL using the engine ID from the user object.
	 * The engine ID format is "{url}|{path}|{engineName}".
	 * Example: "http://localhost:8080|/engine-rest|default"
	 * 
	 * @param user the user object containing the engine ID
	 * @return the complete engine REST URL
	 */
	protected String getEngineRestUrl(CIBUser user) {
		String engineId = user != null ? user.getEngine() : null;
		
		if (engineId != null && !engineId.isEmpty() && engineId.contains("|")) {
			// Parse the engine ID format: "url|path|engineName"
			String[] parts = engineId.split("\\|", 3);
			if (parts.length == 3) {
				String url = parts[0];
				String path = parts[1];
				String engineName = parts[2];
				
				// Build the full URL
				String baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
				String restPath = path.startsWith("/") ? path : "/" + path;
				return baseUrl + restPath + "/engine/" + engineName;
			}
		}
		
		// Fall back to default configuration (for legacy engine names or when no engine specified)
		String baseUrl = cibsevenUrl.endsWith("/") ? cibsevenUrl.substring(0, cibsevenUrl.length() - 1) : cibsevenUrl;
		String restPath = engineRestPath.startsWith("/") ? engineRestPath : "/" + engineRestPath;
		
		// If engine identifier is provided add it to the path
		if (engineId != null && !engineId.isEmpty()) {
			// Legacy format - just use the engine name directly
			return baseUrl + restPath + "/engine/" + engineId;
		}
		
		return baseUrl + restPath;
	}

	/**
	 * Constructs the base engine REST URL without engine context.
	 * Use this for engine-agnostic operations.
	 * @return the base engine REST URL
	 */
	protected String getEngineRestUrl() {
		String baseUrl = cibsevenUrl.endsWith("/") ? cibsevenUrl.substring(0, cibsevenUrl.length() - 1) : cibsevenUrl;
		String restPath = engineRestPath.startsWith("/") ? engineRestPath : "/" + engineRestPath;
		
		return baseUrl + restPath;
	}

	/**
	 * Creates new Http headers and adds Authorization User token
	 * @param user user with authorization to add if null No authorization is added
	 * @return
	 */
	private HttpHeaders createAuthHeader(CIBUser user) {
		HttpHeaders headers =  new HttpHeaders();
		if (user != null) {
		  headers.add(HttpHeaders.AUTHORIZATION, baseUserProvider.getEngineRestToken(user));
		  headers.add(USER_ID_HEADER, user.getId());
		}
		return headers;
	}

	protected <T> ResponseEntity<T> doGet(String url, Class<T> neededClass, CIBUser user, Boolean encoded) {
		try {
			HttpHeaders headers = createAuthHeader(user);
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

			RestTemplate rest = createPatchRestTemplate();

			return rest.exchange(builder.build(encoded).toUri(), HttpMethod.GET, new HttpEntity<>(headers),
					neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	// doGet with accept all headers, some endpoints from engine-rest returns text/plain data or special data so we need to accept it
	protected <T> ResponseEntity<T> doGetWithHeader(String url, Class<T> neededClass, CIBUser user, Boolean encoded, MediaType mediaType) {
		try {
			HttpHeaders headers = createAuthHeader(user);
	        headers.setAccept(Collections.singletonList(mediaType));
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

			// Create a new RestTemplate instance instead of using the shared customRestTemplate
			// This avoids potential ConcurrentModificationExceptions in multi-threaded environments
			RestTemplate rest = createPatchRestTemplate();

			// Add ByteArrayHttpMessageConverter if we're expecting octet-stream data
			// This is necessary for handling binary data like files
			if (MediaType.APPLICATION_OCTET_STREAM.equals(mediaType) && byte[].class.equals(neededClass)) {
				rest.getMessageConverters().add(new org.springframework.http.converter.ByteArrayHttpMessageConverter());
			}

			return rest.exchange(builder.build(encoded).toUri(), HttpMethod.GET, new HttpEntity<>(headers),
					neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}        
	}

	// TODO: Replace this get method from the one above - Main difference is that
	// this method handle special characters.
	protected <T> ResponseEntity<T> doGet(UriComponentsBuilder builder, Class<T> neededClass, CIBUser user) {
		try {
			HttpHeaders headers = createAuthHeader(user);

			RestTemplate rest = createPatchRestTemplate();

			return rest.exchange(builder.build(true).toUri(), HttpMethod.GET, new HttpEntity<>(headers), neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected <T> ResponseEntity<T> doGet(String url, ParameterizedTypeReference<T> neededClass, CIBUser user) {
		try {
			HttpHeaders headers = createAuthHeader(user);
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

			RestTemplate rest = createPatchRestTemplate();

			return rest.exchange(builder.build().toUri(), HttpMethod.GET, new HttpEntity<>(headers), neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected <T> ResponseEntity<T> doPost(String url, Map<String, Object> body, Class<T> neededClass, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

		HttpEntity<Object> request = new HttpEntity<>(body, headers);

		try {
			return customRestTemplate.exchange(builder.build().toUri(), HttpMethod.POST, request, neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected <T> ResponseEntity<T> doPost(String url, ObjectNode body, Class<T> neededClass, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

		HttpEntity<Object> request = new HttpEntity<>(body, headers);

		try {
			return customRestTemplate.exchange(builder.build().toUri(), HttpMethod.POST, request, neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected <T> ResponseEntity<T> doPostParameterized(String url, Map<String, Object> body, ParameterizedTypeReference<T> neededClass, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

		HttpEntity<Object> request = new HttpEntity<>(body, headers);

		try {
			return customRestTemplate.exchange(builder.build().toUri(), HttpMethod.POST, request, neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected <T> ResponseEntity<T> doPost(String url, String body, Class<T> neededClass, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

		HttpEntity<Object> request = new HttpEntity<>(body, headers);
		try {
			ResponseEntity<T> response = customRestTemplate.exchange(builder.build().toUri(), HttpMethod.POST, request, neededClass);
			return response;
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	/**
	 * Performs a POST request with multipart form data
	 * @param url the URL to post to
	 * @param formData the multipart form data containing files and parameters
	 * @param neededClass the expected response class
	 * @param user the user context for authentication
	 * @return the response entity
	 */
	protected <T> ResponseEntity<T> doPostMultipart(String url, MultiValueMap<String, Object> formData, Class<T> neededClass, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(formData, headers);
		try {
			return customRestTemplate.exchange(builder.build().toUri(), HttpMethod.POST, request, neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected void doPut(String url, String body, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		HttpEntity<Object> request = new HttpEntity<>(body, headers);
		try {
			customRestTemplate.put(builder.build().toUri(), request);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected void doPut(String url, Map<String, Object> body, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
		try {
			customRestTemplate.put(builder.build().toUri(), request);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected void doDelete(String url, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		HttpEntity<String> request = new HttpEntity<>("", headers);
		try {
			// rest.delete doesn't work
			customRestTemplate.exchange(builder.build().toUri(), HttpMethod.DELETE, request, String.class);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected <T> ResponseEntity<T> doPatch(String url, Map<String, Object> body, Class<T> neededClass, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
		try {
			// Using customRestTemplate which should be configured to support PATCH
			return customRestTemplate.exchange(builder.build().toUri(), HttpMethod.PATCH, request, neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected <T> ResponseEntity<T> doPatch(String url, String body, Class<T> neededClass, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		HttpEntity<String> request = new HttpEntity<>(body, headers);
		try {
			// Using customRestTemplate which should be configured to support PATCH
			return customRestTemplate.exchange(builder.build().toUri(), HttpMethod.PATCH, request, neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	/**
	 * Creates a new RestTemplate instance configured for PATCH operations.
	 * 
	 * IMPORTANT: This method creates a new RestTemplate instance instead of modifying the shared customRestTemplate.
	 * Modifying the shared RestTemplate would cause ConcurrentModificationException in multi-threaded environments
	 * for the following reasons:
	 * 
	 * 1. RestTemplate's message converters list is not thread-safe for modifications
	 * 2. When multiple threads simultaneously call methods that use RestTemplate (like doGet),
	 *    they can attempt to modify the message converters list at the same time
	 * 3. This causes java.util.ConcurrentModificationException in ArrayList$ArrayListSpliterator.forEachRemaining
	 *    when the RestTemplate tries to iterate through its converters while another thread is modifying them
	 * 
	 * By creating a new RestTemplate instance for each call, we avoid shared state modification
	 * while still benefiting from the configuration of the customRestTemplate by copying its converters.
	 * 
	 * @return A newly configured RestTemplate instance
	 */
	protected RestTemplate createPatchRestTemplate() {
        // Create a new RestTemplate instance instead of modifying the shared one
        RestTemplate restTemplate = new RestTemplate();

        // Configure the ObjectMapper for this specific use case
        ObjectMapper objectMapper = new ObjectMapper();

        StreamReadConstraints streamReadConstraints = StreamReadConstraints
                .builder()
                .maxStringLength(jacksonParserMaxSize)
                .build();
        objectMapper.getFactory().setStreamReadConstraints(streamReadConstraints);

        // Add all converters from the customRestTemplate to the new instance
        // Create a copy of the converters list to avoid concurrent modification issues
		// It's important to create a defensive copy of the shared message converters.
		// This avoids potential ConcurrentModificationExceptions if the shared CustomRestTemplate
		// is accessed by multiple threads simultaneously.
        restTemplate.setMessageConverters(new ArrayList<>(customRestTemplate.getMessageConverters()));

        // Add the custom message converter to the new RestTemplate instance
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter(objectMapper));

        return restTemplate;
	}


	public static Collection<Authorization> filterResources(Collection<Authorization> authorizations, int resourceType) {
		Set<Integer> resourceFilter = Arrays.asList(resourceType).stream().collect(Collectors.toSet());
		return authorizations.stream().filter(authorization -> resourceFilter.contains(authorization.getResourceType())).collect(Collectors.toList());
	}
	protected static RuntimeException wrapException(RuntimeException e, CIBUser user) {
		String technicalErrorMsg = e.getMessage();
		if (technicalErrorMsg == null)
			return e;
		return wrapException(e, technicalErrorMsg, user) ;
	}
	protected static RuntimeException wrapException(HttpStatusCodeException cause, CIBUser user) {
		String technicalErrorMsg = cause.getResponseBodyAsString();
		return wrapException(cause, technicalErrorMsg, user) ;
	}

	protected static RuntimeException wrapException(Throwable cause, String technicalErrorMsg, CIBUser user) {
		RuntimeException wrapperException = null;
		if (technicalErrorMsg.matches(".*Cannot change tenantId of Task.*Tenant id to set.*")) {
			String invalidValue = technicalErrorMsg.replaceFirst(".*Tenant id to set '", "").replaceFirst("'.*", "");
			wrapperException = new InvalidAttributeValueException(Optional.of(invalidValue), cause);
		} else if (technicalErrorMsg.matches(".*Valid values for property.*but was.*")) {
			String invalidValue = technicalErrorMsg.replaceFirst(".*but was '", "").replaceFirst("'.*", "");
			wrapperException = new InvalidAttributeValueException(Optional.of(invalidValue), cause);
		} else if (technicalErrorMsg.matches(".*Cannot set query parameter.*to value.*")) {
			String invalidValue = technicalErrorMsg.replaceFirst(".*to value '", "").replaceFirst("'.*", "");
			wrapperException = new InvalidAttributeValueException(Optional.of(invalidValue), cause);
		} else if (technicalErrorMsg.matches(".*Cannot instantiate process definition.*Unsupported value type.*")) {
			wrapperException = new UnsupportedTypeException(cause);
		} else if (technicalErrorMsg
				.matches(".*Cannot instantiate process definition.*Error while evaluating expression.*")) {
			wrapperException = new ExpressionEvaluationException(cause);
		} else if (technicalErrorMsg.matches(".*Cannot submit task form.*Cannot find task with id.*")) {
			// This check has to precede every other check for "... Cannot find task with id ..."
			wrapperException = new SubmitDeniedException(cause);
		} else if (technicalErrorMsg.matches(".*The user already exists.*")) {
			wrapperException = new ExistingUserRequestException(cause);
		} else if (technicalErrorMsg.matches(".*The group already exists.*")) {
			wrapperException = new ExistingGroupRequestException(cause);
		} else if (technicalErrorMsg.matches(".*The given authenticated user password is not valid.*")) {
			wrapperException = new SystemException(cause); // TODO? Create a specific exception this error.
		} else if (technicalErrorMsg.matches(".*Cannot modify variables for execution.*execution.*doesn't exist: execution is null.*")) {
			wrapperException = new VariableModificationException(technicalErrorMsg, cause);
		} else if (technicalErrorMsg.matches(".*No matching task with id.*")
				|| technicalErrorMsg.matches(".*Process instance with id.*does not exist.*")
				|| technicalErrorMsg.matches(".*Cannot find task with id.*")
				|| technicalErrorMsg.matches(".*Historic process instance with id.*does not exist.*")
				|| technicalErrorMsg.matches(".*No matching definition with id.*")
				|| technicalErrorMsg.matches(".*Cannot get start form data for process definition.*")
				|| technicalErrorMsg.matches(".*Cannot get process instance variable.*execution.*exist.*")
				|| technicalErrorMsg.matches(".*process instance variable with name.*does not exist.*")
				|| technicalErrorMsg.matches(".*Filter with id.*does not exist.*")
				|| technicalErrorMsg.matches(".*Variable instance with Id.*does not exist.*")
				|| technicalErrorMsg.matches(".*Cannot delete execution variable.*execution.*doesn't exist: execution is null.*")) {
			wrapperException = new NoObjectFoundException(cause);
		} else if (technicalErrorMsg.matches(".*OptimisticLockingException.*")) {
			wrapperException = new OptimisticLockingException(cause);
		} else if (technicalErrorMsg.matches(".*task variable with name.*does not exist.*")) {
			wrapperException = new MissingVariableException(cause);
		} else if (technicalErrorMsg.matches(".*Password does not match policy.*")) {
			wrapperException = new PasswordPolicyException(cause);
		} else if (technicalErrorMsg.matches(".*User has an invalid id.*")) {
			wrapperException = new InvalidUserIdException(cause);
		} else if (technicalErrorMsg.matches(".*Null historyTimeToLive values are not allowed.*")) {
			wrapperException = new InvalidValueHistoryTimeToLive(cause);
		} else if (technicalErrorMsg.matches(".*processInstanceIds is empty.*")) {
			wrapperException = new BatchOperationException(technicalErrorMsg, cause);
		} else if (technicalErrorMsg.matches(".*Deployment with id .*does not exist.*")) {
			wrapperException = new WrongDeploymenIdException(cause);
		} else if (technicalErrorMsg.matches(".*Deployment resources for deployment id .*do not exist.*")) {
			wrapperException = new NoRessourcesFoundException(cause);
		} else if (technicalErrorMsg.matches(".*Unable to transform DMN resource.*")) {
			wrapperException = new DmnTransformationException(cause);
		}
		if (wrapperException == null) wrapperException = new SystemException(technicalErrorMsg, cause);
		if (wrapperException instanceof NoObjectFoundException) {
			log.debug("Exception when calling engine-rest:", wrapperException);	
		} else if (!(wrapperException instanceof MissingVariableException)){
			log.error("Exception when calling engine-rest:", wrapperException);	
		}
		return wrapperException;
	}

	protected String addQueryParameter(String param, String name, Optional<String> value, boolean encode) {
		if (value.isPresent()) {
			try {
				String val = encode ? URLEncoder.encode(value.get(), "UTF-8") : value.get();
				return (param.isEmpty() ? "?" : "&") + name + "=" + val;
			} catch (UnsupportedEncodingException e) {
				throw new SystemException(e);
			}
		}
		return "";
	}

	protected String encodeQueryParams(Map<String, Object> queryParams) {
		StringBuilder paramStr = new StringBuilder();
		for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
			if (entry.getValue() != null) {
				paramStr.append(addQueryParameter(paramStr.toString(), entry.getKey(), Optional.ofNullable(entry.getValue().toString()), true));
			}
		}
		return paramStr.toString();
	}

}
