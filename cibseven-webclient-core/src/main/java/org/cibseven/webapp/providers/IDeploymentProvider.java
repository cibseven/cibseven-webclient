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
import java.util.Map;

import org.cibseven.webapp.Data;
import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.Deployment;
import org.cibseven.webapp.rest.model.DeploymentResource;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

public interface IDeploymentProvider {

	public Deployment deployBpmn(MultiValueMap<String, Object> data, MultiValueMap<String, MultipartFile> file, CIBUser user) throws SystemException;
	public Deployment createDeployment(MultiValueMap<String, Object> data, MultipartFile[] files, CIBUser user);
	public Deployment redeployDeployment(String id, Map<String, Object> data, CIBUser user) throws SystemException;
	public Long countDeployments(CIBUser user, String nameLike);
	public Collection<Deployment> findDeployments(CIBUser user, String nameLike, int firstResult, int maxResults, String sortBy, String sortOrder);
	public Collection<DeploymentResource> findDeploymentResources(String deploymentId, CIBUser user);
	public Data fetchDataFromDeploymentResource(HttpServletRequest rq, String deploymentId, String resourceId, String fileName);
	public void deleteDeployment(String deploymentId, Boolean cascade, CIBUser user) throws SystemException;
	public Deployment findDeployment(String deploymentId, CIBUser user) throws SystemException;
	
}
