package org.cibseven.webapp.providers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.providers.VariableProvider;
import org.cibseven.webapp.rest.model.ProcessStart;
import org.cibseven.webapp.rest.model.Variable;
import org.cibseven.webapp.rest.model.VariableHistory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@ContextConfiguration(classes = {VariableProvider.class})
public class VariableProviderIT {

    static {
        System.setProperty("spring.banner.location", "classpath:fca-banner.txt");
    }
	
    @Autowired
    private VariableProvider variableProvider;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String mockBaseUrl = mockWebServer.url("/").toString();
        ReflectionTestUtils.setField(variableProvider, "camundaUrl", mockBaseUrl);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    private String loadMockResponse(String filePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(filePath).toURI())));
    }

    @Test
    void testFetchProcessInstanceVariables() throws Exception {
        String processInstanceId = "process-instance-1";
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        String mockResponseBody = loadMockResponse("mocks/variable_mock.json");

        mockWebServer.enqueue(new MockResponse()
        		.setBody(mockResponseBody)
        		.addHeader("Content-Type", "application/json"));

        Collection<Variable> variables = variableProvider.fetchProcessInstanceVariables(processInstanceId, user, Optional.of(true));

        assertThat(variables).isNotNull();
        assertThat(variables).hasSize(1);
        assertThat(variables.iterator().next().getName()).isEqualTo("var1");
    }

    @Test
    void testFetchActivityVariablesHistory() throws Exception {
        String activityInstanceId = "activity-1";
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");

        String mockResponseBody = loadMockResponse("mocks/variable_history_mock.json");
        mockWebServer.enqueue(new MockResponse()
        		.setBody(mockResponseBody)
        		.addHeader("Content-Type", "application/json"));

        Collection<VariableHistory> history = variableProvider.fetchActivityVariablesHistory(activityInstanceId, user);

        assertThat(history).isNotNull();
        assertThat(history).hasSize(2);

        VariableHistory first = history.iterator().next();
        assertThat(first.getId()).isEqualTo("history-variable-1");
        assertThat(first.getValue()).isEqualTo("value1");
    }

    @Test
    void testSubmitStartFormVariables() throws Exception {
        String processDefinitionId = "definition-1";
        CIBUser user = new CIBUser();
        user.setAuthToken("Bearer token");
        
        ObjectMapper mapper = new ObjectMapper();
        
        List<Variable> formResult = mapper.readValue(
            new File("src/test/resources/mocks/variable_form_result_mock.json"),
            new TypeReference<List<Variable>>() {}
        );
        
        String mockResponseBody = loadMockResponse("mocks/process_start_mock.json");

        mockWebServer.enqueue(new MockResponse().setBody(mockResponseBody).addHeader("Content-Type", "application/json"));

        ProcessStart result = variableProvider.submitStartFormVariables(processDefinitionId, formResult, user);

        assertThat(result).isNotNull();
        assertThat(result.getBusinessKey()).isEqualTo("business-key-1");
    }

}
