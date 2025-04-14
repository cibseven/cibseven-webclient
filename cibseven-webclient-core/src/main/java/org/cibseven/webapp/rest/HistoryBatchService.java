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
import org.cibseven.webapp.rest.model.HistoryBatch;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;

@RestController @RequestMapping("${services.basePath:/services/v1}" + "/history/batch")
public class HistoryBatchService extends BaseService implements InitializingBean {

    @Autowired BpmProvider bpmProvider;
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else throw new SystemException("HistoryBatchService expects a BpmProvider");
	}

    @GetMapping
	public Collection<HistoryBatch> getBatches(
			@RequestParam Map<String, Object> params,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.READ_HISTORY_ALL);
		return bpmProvider.getHistoricBatches(params, user);
	}
    
    @GetMapping("/{id}")
   	public HistoryBatch getHistoricBatchById(
   			@Parameter(description = "Batch id") @PathVariable String id, HttpServletRequest rq) {
   		CIBUser user = checkAuthorization(rq, true, false);
   		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.READ_HISTORY_ALL);
   		return bpmProvider.getHistoricBatchById(id, user);
   	}
    
    @DeleteMapping("/{id}")
   	public void deleteHistoricBatch(
   			@Parameter(description = "Batch id") @PathVariable String id, HttpServletRequest rq) {
   		CIBUser user = checkAuthorization(rq, true, false);
   		checkPermission(user, SevenResourceType.BATCH, PermissionConstants.DELETE_HISTORY_ALL);
   		bpmProvider.deleteHistoricBatch(id, user);
   	}

}