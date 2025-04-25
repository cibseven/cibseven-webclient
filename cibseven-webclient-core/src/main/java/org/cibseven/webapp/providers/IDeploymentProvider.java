package org.cibseven.webapp.providers;

import java.util.Collection;

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
	public Collection<Deployment> findDeployments(CIBUser user);
	public Collection<DeploymentResource> findDeploymentResources(String deploymentId, CIBUser user);
	public Data fetchDataFromDeploymentResource(HttpServletRequest rq, String deploymentId, String resourceId, String fileName);
	public void deleteDeployment(String deploymentId, Boolean cascade, CIBUser user) throws SystemException;
	
}
