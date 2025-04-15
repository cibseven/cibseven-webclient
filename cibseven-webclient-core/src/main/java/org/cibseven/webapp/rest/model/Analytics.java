package org.cibseven.webapp.rest.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) 
public class Analytics {
  
  private List<AnalyticsInfo> runningInstances = new ArrayList<>();
  private List<AnalyticsInfo> openIncidents = new ArrayList<>();
  private List<AnalyticsInfo> openHumanTasks = new ArrayList<>();
	
	private long processDefinitionsCount;
	private long decisionDefinitionsCount;
	private long deploymentsCount;
	private long batchesCount;
	
}
