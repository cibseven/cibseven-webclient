package org.cibseven.webapp.rest;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.BpmProvider;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.Job;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController @RequestMapping("${services.basePath:/services/v1}" + "/job")
public class JobService extends BaseService implements InitializingBean {

    @Autowired BpmProvider bpmProvider;
	SevenProvider sevenProvider;
	
	public void afterPropertiesSet() {
		if (bpmProvider instanceof SevenProvider)
			sevenProvider = (SevenProvider) bpmProvider;
		else throw new SystemException("JobService expects a BpmProvider");
	}

    @PostMapping
	public Collection<Job> getJobs(
			@RequestBody Map<String, Object> params,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		return bpmProvider.getJobs(params, user);
	}

    @PutMapping("/{id}/suspended")
	public void setSuspended(
			@PathVariable String id,
			@RequestBody Map<String, Object> data,
			Locale loc, HttpServletRequest rq) {
		CIBUser user = checkAuthorization(rq, true, false);
		bpmProvider.setSuspended(id, data, user);
	}

}