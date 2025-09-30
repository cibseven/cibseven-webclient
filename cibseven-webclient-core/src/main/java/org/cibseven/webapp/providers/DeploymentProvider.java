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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.cibseven.webapp.Data;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Deployment;
import org.cibseven.webapp.rest.model.DeploymentResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class DeploymentProvider extends SevenProviderBase implements IDeploymentProvider {

	@Override
	public Deployment deployBpmn(MultiValueMap<String, Object> data, MultiValueMap<String, MultipartFile> file, CIBUser user) throws SystemException {
		String url = getEngineRestUrl() + "/deployment/create";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		if (user != null) {
			headers.add(HttpHeaders.AUTHORIZATION, user.getAuthToken());
			headers.add(USER_ID_HEADER, user.getId());
		}
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

		file.forEach((key, value) -> { 
			try {
				data.add(key, value.get(0).getResource());
			} catch (Exception e) {
				throw new SystemException(e);
			}
		});

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(data, headers);

		try {
			return customRestTemplate.exchange(builder.build().toUri(), HttpMethod.POST, request, Deployment.class).getBody();
		} catch (HttpStatusCodeException e) {
			throw wrapException(e, user);
		}

	}

	@Override
	public Deployment createDeployment(MultiValueMap<String, Object> data, MultipartFile[] files, CIBUser user) {
		String url = getEngineRestUrl() + "/deployment/create";
		// Prepare multipart form data - start with provided data
		MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>(data);
		// Add files to form data with indexed "data" keys
		for (int i = 0; i < files.length; i++) {
			MultipartFile file = files[i];
			// API expects files with parameter name "data", so we use indexed keys
			// result could be: ['data0', 'data1', 'data2', ...] or just single ['data']
			String key = files.length > 1 ? "data" + i : "data";
			formData.add(key, file.getResource());
		}
		// Use the base class method for multipart POST
		ResponseEntity<Deployment> response = doPostMultipart(url, formData, Deployment.class, user);
		return response.getBody();
	}

	@Override
	public Long countDeployments(CIBUser user, String nameLike) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getEngineRestUrl() + "/deployment/count");
		if (nameLike != null && !nameLike.isEmpty()) {
			builder.queryParam("nameLike", nameLike);
		}
		String url = builder.toUriString();
		JsonNode response = ((ResponseEntity<JsonNode>) doGet(url, JsonNode.class, user, true)).getBody();
		return response != null ? response.get("count").asLong() : 0L;
	}

  @Override
	public Collection<Deployment> findDeployments(CIBUser user, String nameLike, int firstResult, int maxResults, String sortBy, String sortOrder) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getEngineRestUrl() + "/deployment")
			.queryParam("sortBy", sortBy)
			.queryParam("sortOrder", sortOrder)
			.queryParam("firstResult", firstResult)
			.queryParam("maxResults", maxResults);
		if (nameLike != null && !nameLike.isEmpty()) {
			builder.queryParam("nameLike", nameLike);
		}
		String url = builder.toUriString();
		return Arrays.asList(((ResponseEntity<Deployment[]>) doGet(url, Deployment[].class, user, true)).getBody());
	}
  
  	@Override
  	public Deployment findDeployment(String deploymentId, CIBUser user) {
  	    String url = getEngineRestUrl() + "/deployment/" + deploymentId;
  	    return ((ResponseEntity<Deployment>) doGet(url, Deployment.class, user, false)).getBody();
  	}
	@Override
	public Collection<DeploymentResource> findDeploymentResources(String deploymentId, CIBUser user) {
		String url = getEngineRestUrl() + "/deployment/" + deploymentId + "/resources";
		return Arrays.asList(((ResponseEntity<DeploymentResource[]>) doGet(url, DeploymentResource[].class, user, false)).getBody());
	}

	@Override
	public Data fetchDataFromDeploymentResource(HttpServletRequest rq, String deploymentId, String resourceId, String fileName) {
		String url = getEngineRestUrl() + "/deployment/" + deploymentId + "/resources/" + resourceId + "/data";
		try {
			// Create a CIBUser-like object with just the authorization token
			CIBUser tempUser = null;
			if (rq.getHeader("authorization") != null) {
				tempUser = new CIBUser();
				tempUser.setAuthToken(rq.getHeader("authorization"));
			}

			// Use doGetWithHeader with MediaType.APPLICATION_OCTET_STREAM
			ResponseEntity<byte[]> response = doGetWithHeader(url, byte[].class, tempUser, true, MediaType.APPLICATION_OCTET_STREAM);

			InputStream targetStream = new ByteArrayInputStream(response.getBody());
			InputStreamSource iso = new InputStreamResource(targetStream);
			Data returnValue;
			byte[] body = response.getBody();
			if (body == null)
				throw new NullPointerException();
			MediaType contentType = response.getHeaders().getContentType();
			if (contentType == null)
				throw new NullPointerException();
			returnValue = new Data(fileName, contentType.toString(), iso, body.length);
			return returnValue;

		} catch (HttpStatusCodeException e) {
			throw wrapException(e, null);
		}
	}

	@Override
	public void deleteDeployment(String deploymentId, Boolean cascade, CIBUser user) throws SystemException {
		String url = getEngineRestUrl() + "/deployment/" + deploymentId;
		if(cascade) url += "?cascade=true";
		doDelete(url, user);
	}

}
