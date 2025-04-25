package org.cibseven.webapp.rest.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data @JsonIgnoreProperties(ignoreUnknown = true) 
public class Job {

    private String id;
    private String jobDefinitionId;
    private String dueDate;
    private String processInstanceId;
    private String executionId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private Integer retries;
    private String exceptionMessage;
    private String failedActivityId;
    private Boolean suspended;
    private Long priority;
    private String tenantId;
    private String createTime;
    private String batchId;
}
