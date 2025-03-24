package org.cibseven.providers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.cibseven.Data;
import org.cibseven.auth.CIBUser;
import org.cibseven.exception.SystemException;
import org.cibseven.rest.model.Deployment;
import org.cibseven.rest.model.DeploymentResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class DeploymentProvider extends SevenProviderBase implements IDeploymentProvider {

    @Override
	public Deployment deployBpmn(MultiValueMap<String, Object> data, MultiValueMap<String, MultipartFile> file, CIBUser user) throws SystemException {
		String url = camundaUrl + "/engine-rest/deployment/create";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		if (user != null) headers.add("Authorization", user.getAuthToken());
		
		file.forEach((key, value) -> { 
			try {
				data.add(key, value.get(0).getResource());
			} catch (Exception e) {
				throw new SystemException(e);
			}
		});
		
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(data, headers);
		RestTemplate rest = new RestTemplate();
		
		try {
			return rest.exchange(builder.build().toUri(), HttpMethod.POST, request, Deployment.class).getBody();
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}
		
	}

    @Override
	public Collection<Deployment> findDeployments(CIBUser user) {
		String url = camundaUrl + "/engine-rest/deployment?sortBy=deploymentTime&sortOrder=desc";
		return Arrays.asList(((ResponseEntity<Deployment[]>) doGet(url, Deployment[].class, user, false)).getBody());
	}

	@Override
	public Collection<DeploymentResource> findDeploymentResources(String deploymentId, CIBUser user) {
		String url = camundaUrl + "/engine-rest/deployment/" + deploymentId + "/resources";
		return Arrays.asList(((ResponseEntity<DeploymentResource[]>) doGet(url, DeploymentResource[].class, user, false)).getBody());
	}
   
	@Override
	public Data fetchDataFromDeploymentResource(HttpServletRequest rq, String deploymentId, String resourceId, String fileName) {
		String url = camundaUrl + "/engine-rest/deployment/" + deploymentId + "/resources/" + resourceId + "/data";
		try {
			RestTemplate restTemplate = new RestTemplate();
		    restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
		    HttpHeaders headers = new HttpHeaders();
		    headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
			headers.add("Authorization", rq.getHeader("authorization"));
		    HttpEntity<String> entity = new HttpEntity<String>(headers);
		    ResponseEntity<byte[]> response = restTemplate.exchange(builder.build().toUriString(), HttpMethod.GET, entity, byte[].class, "1");
		   
		    InputStream targetStream = new ByteArrayInputStream(response.getBody());
			InputStreamSource iso = new InputStreamResource(targetStream);
			
			return new Data(fileName, response.getHeaders().getContentType().toString(), iso, response.getBody().length);

		} catch (HttpStatusCodeException e) {
			throw wrapException(e, null);
		}
	}
	
	@Override
	public void deleteDeployment(String deploymentId, Boolean cascade, CIBUser user) throws SystemException {
		String url = camundaUrl + "/engine-rest/deployment/" + deploymentId;
		if(cascade) url += "?cascade=true";
		doDelete(url, user);
	}

	@Override
	protected HttpHeaders addAuthHeader(HttpHeaders headers, CIBUser user) {
		if (user != null) headers.add("Authorization", user.getAuthToken());
		return headers;
	}
	
}
