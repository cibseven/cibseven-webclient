package org.cibseven.providers;

import java.util.Collection;

import org.cibseven.Data;
import org.cibseven.auth.CIBUser;
import org.cibseven.exception.SystemException;
import org.cibseven.rest.model.Deployment;
import org.cibseven.rest.model.DeploymentResource;
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
