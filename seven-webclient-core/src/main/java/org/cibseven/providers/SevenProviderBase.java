package org.cibseven.providers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.cibseven.auth.CIBUser;
import org.cibseven.exception.ExistingGroupRequestException;
import org.cibseven.exception.ExistingUserRequestException;
import org.cibseven.exception.ExpressionEvaluationException;
import org.cibseven.exception.InvalidAttributeValueException;
import org.cibseven.exception.InvalidUserIdException;
import org.cibseven.exception.InvalidValueHistoryTimeToLive;
import org.cibseven.exception.MissingVariableException;
import org.cibseven.exception.NoObjectFoundException;
import org.cibseven.exception.OptimisticLockingException;
import org.cibseven.exception.PasswordPolicyException;
import org.cibseven.exception.SubmitDeniedException;
import org.cibseven.exception.SystemException;
import org.cibseven.exception.UnsupportedTypeException;
import org.cibseven.rest.model.Authorization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SevenProviderBase {
	
	@Value("${custom.spring.jackson.parser.max-size:20000000}") int jacksonParserMaxSize;
	@Value("#{'${camunda.engineRest.url:${camunda.url:}}'}") protected String camundaUrl;
	
	/**
	 * Creates new Http headers and adds User token
	 * @param user
	 * @return
	 */
	protected HttpHeaders createAuthHeader(CIBUser user) {
		HttpHeaders headers =  new HttpHeaders();
		return addAuthHeader(headers, user);
	}
	
	/**
	 * Add Authorization: token to header
	 * @param headers
	 * @param user user with authorization to add if null No authorization is added
	 * @return
	 */
	protected abstract HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user);
	
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
			
			RestTemplate rest = new RestTemplate();
			
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
		
		RestTemplate rest = new RestTemplate();
		
		try {
			return rest.exchange(builder.build().toUri(), HttpMethod.POST, request, neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}
	
	protected <T> ResponseEntity<T> doPostParameterized(String url, Map<String, Object> body, ParameterizedTypeReference<T> neededClass, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		
		HttpEntity<Object> request = new HttpEntity<>(body, headers);
		
		RestTemplate rest = new RestTemplate();
		
		try {
			return rest.exchange(builder.build().toUri(), HttpMethod.POST, request, neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected <T> ResponseEntity<T> doPost(String url, String body, Class<T> neededClass, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

		HttpEntity<Object> request = new HttpEntity<>(body, headers);
		RestTemplate rest = new RestTemplate();
		try {
			ResponseEntity<T> response = rest.exchange(builder.build().toUri(), HttpMethod.POST, request, neededClass);
			return response;
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected void doPut(String url, String body, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		HttpEntity<Object> request = new HttpEntity<>(body, headers);
		RestTemplate rest = new RestTemplate();
		try {
			rest.put(builder.build().toUri(), request);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected void doPut(String url, Map<String, Object> body, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
		RestTemplate rest = new RestTemplate();
		try {
			rest.put(builder.build().toUri(), request);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected void doDelete(String url, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		HttpEntity<String> request = new HttpEntity<>("", headers);
		RestTemplate rest = new RestTemplate();
		try {
			// rest.delete doesn't work
			rest.exchange(builder.build().toUri(), HttpMethod.DELETE, request, String.class);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}
	
	protected <T> ResponseEntity<T> doPatch(String url, Map<String, Object> body, Class<T> neededClass, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
		RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
		try {
			return rest.exchange(builder.build().toUri(), HttpMethod.PATCH, request, neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected <T> ResponseEntity<T> doPatch(String url, String body, Class<T> neededClass, CIBUser user) {
		HttpHeaders headers = createAuthHeader(user);
		headers.setContentType(MediaType.APPLICATION_JSON);

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		HttpEntity<String> request = new HttpEntity<>(body, headers);
		RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
		try {
			return rest.exchange(builder.build().toUri(), HttpMethod.PATCH, request, neededClass);
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
	}

	protected RestTemplate createPatchRestTemplate() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        StreamReadConstraints streamReadConstraints = StreamReadConstraints
                .builder()
                .maxStringLength(jacksonParserMaxSize)
                .build();
        objectMapper.getFactory().setStreamReadConstraints(streamReadConstraints);
    
		RestTemplate rest = new RestTemplateBuilder()
                .additionalMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
		
		return rest;
	}
	
	
	protected Collection<Authorization> filterResources(Collection<Authorization> authorizations, int resourceType) {
		Set<Integer> resourceFilter = Arrays.asList(resourceType).stream().collect(Collectors.toSet());
		return authorizations.stream().filter(authorization -> resourceFilter.contains(authorization.getResourceType())).collect(Collectors.toList());
	}
	
	protected static RuntimeException wrapException(HttpStatusCodeException cause, CIBUser user) {
		String technicalErrorMsg = cause.getResponseBodyAsString();
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
		} else if (technicalErrorMsg.matches(".*No matching task with id.*")
				|| technicalErrorMsg.matches(".*Process instance with id.*does not exist.*")
				|| technicalErrorMsg.matches(".*Cannot find task with id.*")
				|| technicalErrorMsg.matches(".*Historic process instance with id.*does not exist.*")
				|| technicalErrorMsg.matches(".*No matching definition with id.*")
				|| technicalErrorMsg.matches(".*Cannot get start form data for process definition.*")
				|| technicalErrorMsg.matches(".*Cannot get process instance variable.*execution.*exist.*")
				|| technicalErrorMsg.matches(".*process instance variable with name.*does not exist.*")
				|| technicalErrorMsg.matches(".*Filter with id.*does not exist.*")) {
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
		} 
		if (wrapperException == null) wrapperException = new SystemException(technicalErrorMsg, cause);
		if (wrapperException instanceof NoObjectFoundException) {
			log.debug("Exception when calling engine-rest:", wrapperException);	
		} else if (!(wrapperException instanceof MissingVariableException)){
			log.error("Exception when calling engine-rest:", wrapperException);	
		}
		return wrapperException;
	}
	
	protected String addQueryParameter(String param, String name, Optional<String> value) {
		if (value.isPresent()) {
			try {
				String val = URLEncoder.encode(value.get(), "UTF-8");
				return (param.isEmpty() ? "?" : "&") + name + "=" + val;
			} catch (UnsupportedEncodingException e) {
				throw new SystemException(e);
			}
		}
		
		return "";
	}
}
