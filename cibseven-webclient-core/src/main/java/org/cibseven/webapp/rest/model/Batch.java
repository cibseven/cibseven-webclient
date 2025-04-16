package org.cibseven.webapp.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true) 
public class Batch {
    private String id;
    private String type;
    private Long totalJobs;
    private Long batchJobsPerSeed;
    private Long jobsCreated;
    private Long invocationsPerBatchJob;
    private String seedJobDefinitionId;
    private String monitorJobDefinitionId;
    private String batchJobDefinitionId;
    private Long remainingJobs;
    private Long completedJobs;
    private Long failedJobs;
    private Boolean suspended;
    private String tenantId;
    private String createUserId;
    private String startTime;
    private String executionStartTime;
}
