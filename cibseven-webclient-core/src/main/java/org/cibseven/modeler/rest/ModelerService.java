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
package org.cibseven.modeler.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.BaseService;
import org.cibseven.webapp.rest.model.Deployment;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.cibseven.webapp.exception.SystemException;
import org.cibseven.modeler.model.DiagramUsageEntity;
import org.cibseven.modeler.model.FormEntity;
import org.cibseven.modeler.model.FormUsageEntity;
import org.cibseven.modeler.model.UnifiedDiagram;
import org.cibseven.modeler.model.ProcessDiagramEntity;
import org.cibseven.modeler.model.ProcessDiagramReduce;
import org.cibseven.modeler.model.UserSessionEntity;
import org.cibseven.modeler.provider.DBProcessDiagramProvider;
import org.cibseven.modeler.provider.DiagramUsageProvider;
import org.cibseven.modeler.provider.FormProvider;
import org.cibseven.modeler.provider.FormUsageProvider;
import org.cibseven.modeler.provider.UnifiedDiagramProvider;
import org.cibseven.modeler.provider.UserSessionProvider;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.cibseven.modeler.util.ByteArrayMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;

@ApiResponses({
	@ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
	@ApiResponse(responseCode = "401", description = "Unauthorized")
})
@RestController @RequestMapping("${cibseven.webclient.services.basePath:/services/v1}/modeler")
public class ModelerService extends BaseService {

	@Autowired DBProcessDiagramProvider dbProcessDiagramProvider;
	@Autowired DiagramUsageProvider diagramUsageProvider;
	@Autowired FormUsageProvider formUsageProvider;
	@Autowired UserSessionProvider userSessionProvider;
	@Autowired FormProvider formProvider;
	@Autowired UnifiedDiagramProvider unifiedDiagramProvider;

    @Value("${cibsevenmodeler.authentication.enabled:true}")
    private boolean authenticationEnabled;
	
	@RequestMapping(value = "/processes", method = RequestMethod.GET)
	public List<ProcessDiagramReduce> getDiagrams(
		HttpServletRequest rq,
		@RequestParam int firstResult, 
		@RequestParam int maxResults,
		@RequestParam(required = false) String diagramType,
		@RequestParam(required = false) String keyword
	) {
		if (authenticationEnabled) {
			checkAuthentication(rq, true);
		}
		return dbProcessDiagramProvider.getDiagrams(keyword, diagramType, firstResult, maxResults);
	}
	
	@RequestMapping(value = "/unified-diagrams", method = RequestMethod.GET)
	public List<UnifiedDiagram> getUnifiedDiagrams(
		HttpServletRequest rq,
		@RequestParam int firstResult,
		@RequestParam int maxResults,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) String type) {
		if (authenticationEnabled) {
			checkAuthentication(rq, true);
		}
		return unifiedDiagramProvider.getDiagrams(keyword, type, firstResult, maxResults);
	}

	@RequestMapping(value = "/deployment/create", method = RequestMethod.POST)
	public Deployment deployBpmn(
			@Parameter(description = "Metadata of the diagram to be deployed (deployment-name, deployment-source, deploy-changed-only)") @RequestParam MultiValueMap<String, Object> data,
			@Parameter(description = "Diagram to be deployed") @RequestParam MultiValueMap<String, MultipartFile> file,
			HttpServletRequest rq) {
		CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
		return bpmProvider.deployBpmn(data, file, user);
	}
	

	@RequestMapping(value = "/deployment/start/{key}", method = RequestMethod.POST)
	public ProcessStart startProcess(
			@PathVariable String key,
			@RequestBody Map<String, Object> data,			
			HttpServletRequest rq) {
		CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
		return bpmProvider.startProcess(key, null, data, user);
	}
	
	@RequestMapping(value = "/deployment/create/reduce", method = RequestMethod.POST)
	public Deployment deployBpmnReduce(
			@Parameter(description = "Metadata of the diagram to be deployed (deployment-name, deployment-source, deploy-changed-only)") @RequestParam String deploymentName,
			@Parameter(description = "Diagram to be deployed") @RequestParam MultiValueMap<String, MultipartFile> file,
			HttpServletRequest rq) {
		CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
		ProcessDiagramEntity diagramEntity = dbProcessDiagramProvider.findByProcessKey(deploymentName);
		if (diagramEntity == null) throw new SystemException(new IllegalArgumentException("Diagram not found for key: " + deploymentName));
		String type = diagramEntity.getType();
		if (type.contains("bpmn")) type = "bpmn";
	    MultiValueMap<String, MultipartFile> multiValueMapFile = byteArrayToMultiValueMap(file.getFirst("file"), deploymentName, type);
	    return bpmProvider.deployBpmn(createDefaultmultiValueMap(deploymentName), multiValueMapFile, user);
	}
	
	@RequestMapping(value = "/process/create", method = RequestMethod.POST)
	public ProcessDiagramEntity createProcessFromFile(@Parameter(description = "Process diagram to be created") @RequestParam MultiValueMap<String, MultipartFile> file, @RequestParam boolean overwrite,HttpServletRequest rq) throws Exception {
		CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
		ProcessDiagramEntity entity = new ProcessDiagramEntity();
		MultipartFile multipartFile = file.getFirst("file");
		if (multipartFile == null) throw new SystemException(new IllegalArgumentException("Uploaded file is missing"));
		String originalFilename = multipartFile.getOriginalFilename() != null ? multipartFile.getOriginalFilename() : "";
		String tagName = originalFilename.endsWith(".dmn") ? "decision" : "process";
		String type = originalFilename.endsWith(".dmn") ? "dmn" : "bpmn-c7";
		ProcessDiagramEntity artifactExists = null;
		if (multipartFile != null) {
			try {
				byte[] fileContent = multipartFile.getBytes();                
	            String prefix = getBpmnPrefix(fileContent);
	    		entity.setName(getXmlAttribute(fileContent, tagName.equals("process") ? prefix + tagName : tagName, "name"));
	    		entity.setProcesskey(getXmlAttribute(fileContent, tagName.equals("process") ? prefix + tagName : tagName, "id"));
	    		entity.setActive(true);
				entity.setDiagram(fileContent);
				entity.setType(type);
				artifactExists = dbProcessDiagramProvider.findByName(entity.getName());
				
				if(artifactExists != null && overwrite) {
					entity.setId(artifactExists.getId());
					artifactExists.setDiagram(fileContent);
					artifactExists.setUpdatedBy(userIdFrom(user));
					return dbProcessDiagramProvider.updateDiagram(artifactExists);
				}
				
	        } catch (IOException e) {}
		}
		entity.setUpdatedBy(userIdFrom(user));
		return dbProcessDiagramProvider.createDiagram(entity);			
	}

	@RequestMapping(value = "/deployment/create/{id}", method = RequestMethod.POST)
	public Deployment deployBpmnById(
			/*@Parameter(description = "Metadata of the diagram to be deployed (deployment-name, deployment-source, deploy-changed-only)") @RequestParam Optional<MultiValueMap<String, Object>> data,*/
			@Parameter(description = "id of the diagram") @PathVariable String id,
			HttpServletRequest rq) {
		CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
		ProcessDiagramEntity entity = dbProcessDiagramProvider.findById(id).get();
		byte[] bytes = entity.getDiagram(); 
		String type = entity.getType();
		if (type.contains("bpmn")) type = "bpmn";
		MultiValueMap<String, MultipartFile> file = byteArrayToMultiValueMap(bytes, entity.getProcesskey(), type);
		return bpmProvider.deployBpmn(createDefaultmultiValueMap(entity.getProcesskey()), file, user);
	}
	
	private MultiValueMap<String, MultipartFile> byteArrayToMultiValueMap(byte[] bytes, String fileName, String type) {
	    MultipartFile multipartFile = new ByteArrayMultipartFile(fileName, fileName + "." + type, "application/octet-stream", bytes);
	    MultiValueMap<String, MultipartFile> multiValueMap = new LinkedMultiValueMap<>();
	    multiValueMap.add(fileName + "." + type, multipartFile);
	    return multiValueMap;
	}

	private MultiValueMap<String, MultipartFile> byteArrayToMultiValueMap(MultipartFile file, String fileName, String type) {
		try {
			byte[] bytes = file.getBytes();
			return byteArrayToMultiValueMap(bytes, fileName, type);
		} catch (IOException e) {
			throw new SystemException(e);
		}
	}
	
	private MultiValueMap<String, Object> createDefaultmultiValueMap(String name) {
	    MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
	    multiValueMap.add("deployment-name", name);
	    multiValueMap.add("deployment-source", "CIB Seven process management");
	    multiValueMap.add("enable-duplicate-filtering", true);
	    return multiValueMap;			
	}
	
	@RequestMapping(value = "/deployment", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public void checkDeployBpmn(HttpServletRequest rq) {
		if (authenticationEnabled) {
			checkAuthentication(rq, true);
		}
	}

	@RequestMapping(value = "/process/save", method = RequestMethod.POST)
	public ProcessDiagramEntity save(@RequestParam MultiValueMap<String, String> data, @RequestParam MultiValueMap<String, MultipartFile> diagram, HttpServletRequest rq) {
		CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
		ProcessDiagramEntity entity = new ProcessDiagramEntity();
		if (data.containsKey("name")) entity.setName(data.get("name").get(0).toString());
		if (data.containsKey("processkey")) entity.setProcesskey(data.get("processkey").get(0).toString());
		if (data.containsKey("description")) entity.setDescription(data.get("name").get(0).toString());
		if (data.containsKey("active")) entity.setActive(true);
		if (data.containsKey("type")) entity.setType(data.get("type").get(0).toString());
		else entity.setActive(true);
		try {
			entity.setDiagram(diagram.getFirst("diagram").getInputStream().readAllBytes());
		} catch (IOException e) {
			entity.setDiagram(null);
		}
		entity.setUpdatedBy(userIdFrom(user));
		return dbProcessDiagramProvider.createDiagram(entity);
	}

	@Transactional
	@RequestMapping(value = "/session/save", method = RequestMethod.POST)
	public Object saveSession(@RequestParam MultiValueMap<String, String> data,
			@RequestParam MultiValueMap<String, MultipartFile> diagram, HttpServletRequest rq) {
		CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
		
		try {
			UserSessionEntity sessionEntity = new UserSessionEntity();
			sessionEntity.setUserId(user != null ? user.getUserID() : "anonymous");

			UserSessionEntity newSession = userSessionProvider.createSession(sessionEntity);
			
			 String type = data.getFirst("type"); 
		        if ("form".equalsIgnoreCase(type)) {
		            FormUsageEntity newFormUsageEntity = new FormUsageEntity();
		            newFormUsageEntity.setUserId(user != null ? user.getUserID() : "anonymous");
		            if (data.containsKey("id")) {
		                String formId = data.get("id").get(0);
		                FormEntity formEntity = formProvider.findById(formId)
		                        .orElseThrow(() -> new RuntimeException("Form not found"));
		                newFormUsageEntity.setUserSession(newSession);
		                newFormUsageEntity.setForm(formEntity);
		            }
		            return formUsageProvider.createFormUsage(newFormUsageEntity);
		        } else {
		            DiagramUsageEntity newDiagramUsageEntity = new DiagramUsageEntity();
		            newDiagramUsageEntity.setUserId(user != null ? user.getUserID() : "anonymous");
		            if (data.containsKey("id")) {
		                String diagramId = data.get("id").get(0);
		                ProcessDiagramEntity diagramEntity = dbProcessDiagramProvider.findById(diagramId)
		                        .orElseThrow(() -> new RuntimeException("Diagram not found"));

		                newDiagramUsageEntity.setUserSession(newSession);
		                newDiagramUsageEntity.setDiagram(diagramEntity);
		            }
		            return diagramUsageProvider.createDiagramUsage(newDiagramUsageEntity);
		        }
		    } catch (Exception e) {
		        throw e;
		    }
	}
	
	@Transactional
	@RequestMapping(value = "/session/close", method = RequestMethod.POST)
	public ResponseEntity<Object> closeSessions(@RequestParam MultiValueMap<String, String> data,
		HttpServletRequest rq) {

		if (authenticationEnabled) {
			checkAuthentication(rq, true);
		}

		String sessionIdsString = data.getFirst("sessionId");
		if (sessionIdsString != null && data.containsKey("type")) {
			String[] sessionIds = sessionIdsString.split(",");
			String type = data.get("type").get(0);
			for (String id : sessionIds) {
				if (type.equals("form")) {
					FormUsageEntity existingFormUsageEntity = formUsageProvider.findBySessionId(id);
					if (existingFormUsageEntity != null) {
						formUsageProvider.closeSession(existingFormUsageEntity);
					}
				} else {
					DiagramUsageEntity existingDiagramUsageEntity = diagramUsageProvider.findBySessionId(id);
					if (existingDiagramUsageEntity != null) {
						diagramUsageProvider.closeSession(existingDiagramUsageEntity);
					}
				}
			}
		}

		return ResponseEntity.ok().build();
	}
	
	@RequestMapping(value = "/process/session/check/{id}", method = RequestMethod.GET)
	public ResponseEntity<Map<String, String>> checkProcessSession(@PathVariable String id,  HttpServletRequest rq) {

		CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
		DiagramUsageEntity newDiagramUsageEntity = diagramUsageProvider.checkSessionUser(id);
		Map<String, String> response = new HashMap<>();
		
		if (newDiagramUsageEntity == null) {
			response.put("message", "NO_SESSION");
	        return ResponseEntity.ok(response);
		}
		//response.put("entity", newDiagramUsageEntity);
		response.put("userId", newDiagramUsageEntity.getUserId());
		response.put("openedAt", newDiagramUsageEntity.getOpenedAt().toString());
		UserSessionEntity userSession = new UserSessionEntity();
        userSession.setId(newDiagramUsageEntity.getSessionId());
        newDiagramUsageEntity.setUserSession(userSession);
		response.put("sessionId", userSession.getId());
		
		if (user != null && user.getUserID().equals(newDiagramUsageEntity.getUserId())) {
			response.put("message", "SAME_USER");			
			return ResponseEntity.ok(response);
		}
		response.put("message", "SESSION_FOUND");
	    return ResponseEntity.ok(response);
	}
	
	@RequestMapping(value = "/form/session/check/{id}", method = RequestMethod.GET)
	public ResponseEntity<Map<String, String>> checkFormSession(@PathVariable String id,  HttpServletRequest rq) {
		
		CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
		FormUsageEntity newFormUsageEntity = formUsageProvider.checkSessionUser(id);
		Map<String, String> response = new HashMap<>();
		
		if (newFormUsageEntity == null) {
			response.put("message", "NO_SESSION");
	        return ResponseEntity.ok(response);
		}
		
		//response.put("entity", newFormUsageEntity.getSessionId());
		response.put("userId", newFormUsageEntity.getUserId());
		response.put("openedAt", newFormUsageEntity.getOpenedAt().toString());
		UserSessionEntity userSession = new UserSessionEntity();
        userSession.setId(newFormUsageEntity.getSessionId());
        newFormUsageEntity.setUserSession(userSession);
		response.put("sessionId", userSession.getId());
		
		if (user != null && user.getUserID().equals(newFormUsageEntity.getUserId())) {
			response.put("message", "SAME_USER");
			return ResponseEntity.ok(response);
		}
		response.put("message", "SESSION_FOUND");
	    return ResponseEntity.ok(response);
	}

	@RequestMapping(value = "/process/save/object", method = RequestMethod.POST)
	public ProcessDiagramEntity saveProject(@RequestBody ProcessDiagramEntity data, HttpServletRequest rq) {
		CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
		String updatedBy = userIdFrom(user);
		Optional<ProcessDiagramEntity> entity = dbProcessDiagramProvider.findById(data.getId());
		
		if (entity.isEmpty()) {
			ProcessDiagramEntity newEntity = new ProcessDiagramEntity();
			newEntity.setName(data.getName());
			newEntity.setProcesskey(data.getProcesskey());
			newEntity.setDescription(data.getDescription());
			newEntity.setActive(data.getActive());
			newEntity.setType(data.getType());
			newEntity.setDiagram(data.getDiagram());
			newEntity.setUpdatedBy(updatedBy);
			return dbProcessDiagramProvider.createDiagram(newEntity);
		} else {
			entity.get().setName(data.getName());
			entity.get().setProcesskey(data.getProcesskey());
			entity.get().setDescription(data.getDescription());
			entity.get().setActive(data.getActive());
			entity.get().setType(data.getType());
			entity.get().setDiagram(data.getDiagram());
			entity.get().setUpdatedBy(updatedBy);
			return dbProcessDiagramProvider.updateDiagram(entity.get());
		}
	}
	
	@Transactional
	@RequestMapping(value = "/process/update", method = RequestMethod.POST)
	public ProcessDiagramEntity update(@RequestParam MultiValueMap<String, String> data, @RequestParam MultiValueMap<String, MultipartFile> diagram, HttpServletRequest rq) {
		CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
		ProcessDiagramEntity entity = new ProcessDiagramEntity();
		if (data.containsKey("id")) entity.setId(data.get("id").get(0).toString());
		if (data.containsKey("name")) entity.setName(data.get("name").get(0).toString());
		if (data.containsKey("processkey")) entity.setProcesskey(data.get("processkey").get(0).toString());
		if (data.containsKey("description")) entity.setDescription(data.get("name").get(0).toString());
		if (data.containsKey("active")) entity.setActive(true);
		if (data.containsKey("type")) entity.setType(data.get("type").get(0).toString());
		else entity.setActive(true);
		try {
			entity.setDiagram(diagram.getFirst("diagram").getInputStream().readAllBytes());
		} catch (IOException e) {
			entity.setDiagram(null);
		}
		entity.setUpdatedBy(userIdFrom(user));

		ProcessDiagramEntity updatedDiagram = dbProcessDiagramProvider.updateDiagram(entity);
		
		if (data.containsKey("id") && updatedDiagram != null && user != null) { //renew session
			DiagramUsageEntity diagramUsage = diagramUsageProvider.checkSessionUser(data.get("id").get(0).toString());
			if (diagramUsage != null && diagramUsage.getUserId().equals(user.getUserID())) {
				diagramUsageProvider.createDiagramUsage(diagramUsage);
			}
		}
		return updatedDiagram;
	}
	
	@RequestMapping(value = "/process/find-by-name/data", method = RequestMethod.POST)
	public ResponseEntity<byte[]> findByName(@RequestParam String name, HttpServletRequest rq) {
		if (authenticationEnabled) {
			checkAuthentication(rq, true);
		}
		ProcessDiagramEntity diagram = dbProcessDiagramProvider.findByName(name);
		if (diagram == null) return ResponseEntity.notFound().build();
		byte[] file = diagram.getDiagram();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
		return new ResponseEntity<>(file, headers, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/process/find-by-key/data", method = RequestMethod.POST)
	public ResponseEntity<byte[]> findByKey(@RequestParam String key, HttpServletRequest rq) {
		if (authenticationEnabled) {
			checkAuthentication(rq, true);
		}
		ProcessDiagramEntity diagram = dbProcessDiagramProvider.findByProcessKey(key);
		if (diagram == null) return ResponseEntity.notFound().build();
		byte[] file = diagram.getDiagram();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
		return new ResponseEntity<>(file, headers, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/process/{id}/data", method = RequestMethod.GET)
	public ResponseEntity<byte[]> findByIdData(@PathVariable String id,  HttpServletRequest rq) {
		if (authenticationEnabled) {
			checkAuthentication(rq, true);
		}
		ProcessDiagramEntity entity = dbProcessDiagramProvider.findById(id).orElse(null);
		if (entity == null) {
			return ResponseEntity.notFound().build();
		}
		byte[] file = entity.getDiagram();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
		ResponseEntity<byte[]> response = new ResponseEntity<>(file, headers, HttpStatus.OK);
		return response;
	}

	@RequestMapping(value = "/process/{id}", method = RequestMethod.GET)
	public ResponseEntity<ProcessDiagramEntity> findById(@PathVariable String id, HttpServletRequest rq) {
		if (authenticationEnabled) {
			checkAuthentication(rq, true);
		}
		ProcessDiagramEntity entity = dbProcessDiagramProvider.findById(id).orElse(null);
		if (entity == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(entity);
	}
	
	@RequestMapping(value = "/process/delete/{id}", method = RequestMethod.DELETE)
	public void delete(@PathVariable String id, HttpServletRequest rq) {
		if (authenticationEnabled) {
			checkAuthentication(rq, true);
		}
		dbProcessDiagramProvider.delete(id);
	}
	
	@RequestMapping(value = "/form/save", method = RequestMethod.POST)
	public FormEntity saveForm(@RequestParam("formid") String formid, @RequestParam("form_schema") MultipartFile formSchema, HttpServletRequest rq) {
		CIBUser user = null;
	    if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
	    FormEntity entity = new FormEntity();
	    entity.setFormId(formid); 
	    
	    try {
	        entity.setFormSchema(formSchema.getBytes());
	    } catch (IOException e) {
	        entity.setFormSchema(null);
	    }
	    entity.setUpdatedBy(userIdFrom(user));
	    return formProvider.createForm(entity);
	}
	
	@Transactional
	@RequestMapping(value = "/form/delete/{id}", method = RequestMethod.DELETE)
	public void deleteForm(@PathVariable String id, HttpServletRequest rq) {
		if (authenticationEnabled) {
			checkAuthentication(rq, true);
		}
		formProvider.delete(id);
	}
	
	@Transactional
	@RequestMapping(value = "/form/update", method = RequestMethod.POST)
	public FormEntity updateForm(@RequestParam("id") String id, @RequestParam("formid") String formid, 
		@RequestParam("form_schema") MultipartFile formSchema, HttpServletRequest rq) {
	    CIBUser user = null;
		if (authenticationEnabled) {
			user = checkAuthentication(rq, true);
		}
	    FormEntity entity = new FormEntity();
	    entity.setFormId(formid); 
	    entity.setId(id);	    
	    try {
	        entity.setFormSchema(formSchema.getBytes());
	    } catch (IOException e) {
	        entity.setFormSchema(null);
	    }
	    entity.setUpdatedBy(userIdFrom(user));

	    FormEntity updatedForm = formProvider.updateForm(entity);
	    
	    if (updatedForm != null && user != null) { //renew session
			FormUsageEntity formUsage = formUsageProvider.checkSessionUser(id);
			if (formUsage != null && formUsage.getUserId().equals(user.getUserID())) {
				formUsageProvider.createFormUsage(formUsage);
			}
		}
		return updatedForm;
	}
	
	@RequestMapping(value = "/forms", method = RequestMethod.GET)
	public List<FormEntity> getForms(
		HttpServletRequest rq,
		@RequestParam int firstResult,
		@RequestParam int maxResults,
		@RequestParam(required = false) String keyword) {
		if (authenticationEnabled) {
			checkAuthentication(rq, true);
		}
		return formProvider.getForms(keyword, firstResult, maxResults);
	}
	
	@RequestMapping(value = "/form/{id}/data", method = RequestMethod.GET)
	public ResponseEntity<byte[]> findFormById(@PathVariable String id,  HttpServletRequest rq) {
		if (authenticationEnabled) {
			checkAuthentication(rq, true);
		}
		FormEntity form = formProvider.findById(id).orElse(null);
		if (form == null) return ResponseEntity.notFound().build();
		byte[] file = form.getFormSchema();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new ResponseEntity<>(file, headers, HttpStatus.OK);
	}

	private static String userIdFrom(CIBUser user) {
		return user != null ? user.getUserID() : null;
	}

	private static String getXmlAttribute(byte[] xmlBytes, String tagName, String propertyName) throws Exception {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlBytes);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputStream);
		NodeList nodeList = document.getElementsByTagName(tagName);
		if (nodeList.getLength() > 0) {
			Element processElement = (Element) nodeList.item(0);
			return processElement.getAttribute(propertyName);
		}
        return null;
    }

	private static String getBpmnPrefix(byte[] xmlBytes) throws Exception {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlBytes);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(inputStream);

		String[] prefixes = {"bpmn:", "bpmn2:"};
		for (String prefix : prefixes) {
			NodeList list = document.getElementsByTagName(prefix + "process");
			if (list.getLength() > 0) {
				return prefix;
			}
		}
		return "";
	}
}
