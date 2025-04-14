package org.cibseven.webapp.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.auth.SevenUserProvider;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.cibseven.webapp.providers.ActivityProvider;
import org.cibseven.webapp.providers.BatchProvider;
import org.cibseven.webapp.providers.DecisionProvider;
import org.cibseven.webapp.providers.DeploymentProvider;
import org.cibseven.webapp.providers.FilterProvider;
import org.cibseven.webapp.providers.IncidentProvider;
import org.cibseven.webapp.providers.JobDefinitionProvider;
import org.cibseven.webapp.providers.JobProvider;
import org.cibseven.webapp.providers.ProcessProvider;
import org.cibseven.webapp.providers.SevenProvider;
import org.cibseven.webapp.providers.SystemProvider;
import org.cibseven.webapp.providers.TaskProvider;
import org.cibseven.webapp.providers.UserProvider;
import org.cibseven.webapp.providers.UtilsProvider;
import org.cibseven.webapp.providers.VariableProvider;
import org.cibseven.webapp.rest.model.Analytics;
import org.cibseven.webapp.rest.model.AnalyticsInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(properties = {"camunda.engineRest.url=http://192.168.96.236:8080"})
@ContextConfiguration(classes = {
    AnalyticsService.class,
    SevenProvider.class,
    DeploymentProvider.class,
    VariableProvider.class,
    TaskProvider.class,
    ProcessProvider.class,
    IncidentProvider.class,
    ActivityProvider.class,
    FilterProvider.class,
    UtilsProvider.class,
    JobDefinitionProvider.class,
    UserProvider.class,
    DecisionProvider.class,
    JobProvider.class,
    BatchProvider.class,
    SystemProvider.class,
    SevenUserProvider.class,
    })
public class AnalyticsServiceIT {
  
  @Autowired
  private SevenUserProvider sevenUserProvider;
  
  @Autowired
  private AnalyticsService analyticsService;
  
  @Test
  public void testAnalytics() {
    
    int expectedRunningInstances = 7;
    int expectedOpenIncidents = 2;
    int expectedOpenHumanTasks = 4;
    int expectedNumberOfInvoiceReceiptHumanTasks = 37;
    
    int expectedDecisionsCount = 3;
    int expectedDeploymentsCount = 24;
    
    StandardLogin login = new StandardLogin("demo", "demo");
    
    CIBUser user = sevenUserProvider.login(login, null);
    
    Analytics analytics = analyticsService.getAnalytics(Locale.ENGLISH, user);
    
    // Assert
    assertThat(analytics).isNotNull();
    
    List<AnalyticsInfo> runningInstances = analytics.getRunningInstances();
    
    assertThat(runningInstances).isNotNull().isNotEmpty();
    assertThat(runningInstances.size()).isEqualTo(expectedRunningInstances);
    
    // Check that runningInstances contains AnalyticsInfo with id "invoice" and title "Invoice Receipt"
    AnalyticsInfo invoiceInstance = runningInstances.stream()
        .filter(instance -> "invoice".equals(instance.getId()))
        .findFirst()
        .orElse(null);
    
    assertThat(invoiceInstance).isNotNull();
    assertThat(invoiceInstance.getTitle()).isEqualTo("Invoice Receipt");
    
    
    
    List<AnalyticsInfo> openIncidents = analytics.getOpenIncidents();
    
    assertThat(openIncidents).isNotNull().isNotEmpty();
    assertThat(openIncidents.size()).isEqualTo(expectedOpenIncidents);
    
    // Check that first openIncident title is "Second Incident Task Script"
    AnalyticsInfo openIncident = openIncidents.stream()
        .findFirst()
        .orElse(null);
    
    assertThat(openIncident).isNotNull();
    assertThat(openIncident.getTitle()).isEqualTo("Second Incident Task Script");
    
    
    List<AnalyticsInfo> openHumanTasks = analytics.getOpenHumanTasks();
    
    assertThat(openHumanTasks).isNotNull().isNotEmpty();
    assertThat(openHumanTasks.size()).isEqualTo(expectedOpenHumanTasks);
    
    // Assert that the number of openHumanTasks with the name "Invoice Receipt" is 37
    AnalyticsInfo invoiceReceiptProcess = openHumanTasks.stream()
        .filter(process -> "Invoice Receipt".equals(process.getTitle()))
        .findFirst()
        .orElse(null);

    assertThat(invoiceReceiptProcess.getValue()).isEqualTo(expectedNumberOfInvoiceReceiptHumanTasks);
    

    long decisionDefinitionsCount = analytics.getDecisionDefinitionsCount();
    
    assertThat(decisionDefinitionsCount).isEqualTo(expectedDecisionsCount);
    
    long deploymentsCount = analytics.getDeploymentsCount();
    
    assertThat(deploymentsCount).isEqualTo(expectedDeploymentsCount);
    
  }

}
