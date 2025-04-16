package org.cibseven.webapp.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Locale;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.AnalyticsService;
import org.cibseven.webapp.rest.BaseService;
import org.cibseven.webapp.rest.model.Authorizations;
import org.cibseven.webapp.rest.model.ProcessStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

class AnalyticsServiceTest {

  @Mock
  private SevenProviderBase sevenProviderBase;
  
  @Mock
  private UserProvider userProvider;
  
  @Mock
  private BpmProvider bpmProvider;

  @InjectMocks
  private AnalyticsService analyticsService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  
  @Test
  void testAnalyticsFunctionality() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    
// // Use reflection to access the private field in the superclass (BaseService)
//    var field = BaseService.class.getDeclaredField("bpmProvider");
//    field.setAccessible(true);
//
//    // Get the value of the field from the analyticsService instance
//    Object injectedBpmProvider = field.get(analyticsService);
//
//    // Assert that bpmProvider was injected
//    assertNotNull(injectedBpmProvider, "bpmProvider should be injected into analyticsService");
//    assertEquals(bpmProvider, injectedBpmProvider, "Injected bpmProvider should match the mock");
    
    
 // Mock required arguments
    UriComponentsBuilder uriBuilder = mock(UriComponentsBuilder.class);
    ParameterizedTypeReference<ProcessStatistics[]> responseType = new ParameterizedTypeReference<>() {};
    CIBUser user = mock(CIBUser.class);
    
    // Create mock response
//    Authorizations mockAuthorizations = new Authorizations(); // mock(Authorizations.class);
    
    // Create a mock Authorizations object
    Authorizations mockAuthorizations = mock(Authorizations.class);

    // Configure the mock to return non-null collections or "true" for permission checks
    when(mockAuthorizations.getProcessDefinition()).thenReturn(Collections.emptyList());
//    when(mockAuthorizations.getApplication()).thenReturn(Collections.emptyList());
//    when(mockAuthorizations.getTask()).thenReturn(Collections.emptyList());
    // Add similar stubs for other methods if needed
    
    // Mock the behavior of bpmProvider
    when(userProvider.getUserAuthorization(any(String.class), eq(user)))
        .thenReturn(mockAuthorizations);

    // Create mock response
    ProcessStatistics[] mockStatistics = { new ProcessStatistics() }; // Add mock data as needed
    ResponseEntity<ProcessStatistics[]> mockResponse = ResponseEntity.ok(mockStatistics);

    // Mock the behavior of sevenProviderBase
    when(sevenProviderBase.doGet(
            eq("/engine-rest/process-definition/statistics?failedJobs=true"),
            eq((Class<ProcessStatistics[]>) (Class<?>) ProcessStatistics[].class),
            any(CIBUser.class),
            anyBoolean()))
        .thenReturn(mockResponse);

    // Call the method to test
    var result = analyticsService.getAnalytics(Locale.ENGLISH, user);

    // Verify the result
    assertEquals(1, result);
    verify(sevenProviderBase).doGet(uriBuilder, responseType.getClass(), user);
  }
  
}