package org.cibseven.webapp.rest;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.Batch;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController @RequestMapping("${services.basePath:/services/v1}" + "/batch")
public class BatchService extends BaseService implements InitializingBean {

    @Autowired BpmProvider bpmProvider;
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else throw new SystemException("BatchService expects a BpmProvider");
	}

    @GetMapping
	public Collection<Batch> getBatches(
			@RequestParam Map<String, Object> params,
			HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.READ_ALL);
		return bpmProvider.getBatches(params, user);
	}
    
    @GetMapping("/statistics")
	public Collection<Batch> getBatchStatistics(
			@RequestParam Map<String, Object> params,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.READ_ALL);
		return bpmProvider.getBatchStatistics(params, user);
	}
    
    @DeleteMapping("/{id}")
   	public void deleteBatch(
   			@PathVariable String id,
			@RequestParam Map<String, Object> params,
			HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.DELETE_ALL);
		bpmProvider.deleteBatch(id, params, user);
	}
    
    @PutMapping("/{id}/suspended")
   	public void setBatchSuspensionState(
   			@PathVariable String id,
   			@RequestBody Map<String, Object> params,
			HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.UPDATE_ALL);
		bpmProvider.setBatchSuspensionState(id, params, user);
	}

}