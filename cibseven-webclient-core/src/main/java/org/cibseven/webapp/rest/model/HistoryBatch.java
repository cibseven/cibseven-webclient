package org.cibseven.webapp.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data @JsonIgnoreProperties(ignoreUnknown = true) 
public class HistoryBatch {
    private String id;
    private String type;
    private Long batchJobsPerSeed;
    private Long invocationsPerBatchJob;
    private String seedJobDefinitionId;
    private String monitorJobDefinitionId;
    private String batchJobDefinitionId;
    private String tenantId;
    private String createUserId;
    private Long totalJobs;  
    private String startTime;
    private String endTime;
    private String executionStartTime;
    private String removalTime;
}
