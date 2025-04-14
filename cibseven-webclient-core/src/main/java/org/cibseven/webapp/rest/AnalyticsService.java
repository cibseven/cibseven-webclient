package org.cibseven.webapp.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenResourceType;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.providers.PermissionConstants;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.rest.model.Analytics;
import org.cibseven.webapp.rest.model.AnalyticsInfo;
import org.cibseven.webapp.rest.model.ProcessStatistics;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@ApiResponses({ @ApiResponse(responseCode = "500", description = "An unexpected system error occured"),
    @ApiResponse(responseCode = "401", description = "Unauthorized") })
@RestController
@RequestMapping("${services.basePath:/services/v1}" + "/analytics")
public class AnalyticsService extends BaseService implements InitializingBean {

  SevenProvider sevenProvider;

  public void afterPropertiesSet() {
    if (bpmProvider instanceof SevenProvider)
      sevenProvider = (SevenProvider) bpmProvider;
    else
      throw new SystemException("ProcessService expects a BpmProvider");
  }

  @Operation(summary = "Get analytics for processes, decisions and human Tasks", description = "<strong>Return: Analytics")
  @RequestMapping(value = "", method = RequestMethod.GET)
  public Analytics getAnalytics(Locale loc, CIBUser user) {
    checkPermission(user, SevenResourceType.PROCESS_DEFINITION, PermissionConstants.READ_ALL);
    Collection<ProcessStatistics> processStatistics = sevenProvider.getProcessStatistics(user);
    Analytics analytics = new Analytics();

    List<AnalyticsInfo> runningInstances = new ArrayList<>();
    
    // Group by the first part of the processInstanceId and summarize instances
    Map<String, Long> groupedInstances = processStatistics.stream()
        .collect(Collectors.groupingBy(
            stats -> stats.getId().split(":")[0], // Group by the first part of the ID
            Collectors.summingLong(ProcessStatistics::getInstances) // Summarize instances
        ));

    // Convert the grouped results into IncidentInfo objects and add to runningInstances
    for (Map.Entry<String, Long> entry : groupedInstances.entrySet()) {
        AnalyticsInfo incidentInfo = new AnalyticsInfo();
        incidentInfo.setTitle(entry.getKey());
        incidentInfo.setValue(entry.getValue());
        runningInstances.add(incidentInfo);
    }

    analytics.setRunningInstances(runningInstances);

    // ToDo: Business Logic here

    return analytics;
  }
}