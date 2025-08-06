/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.cibseven.webapp.rest.model;


import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricDecisionInstance {
    private String id;
    private String decisionDefinitionId;
    private String decisionDefinitionKey;
    private String decisionDefinitionName;
    private String evaluationTime;
    private String removalTime;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processInstanceId;
    private String caseDefinitionId;
    private String caseDefinitionKey;
    private String caseInstanceId;
    private String activityId;
    private String activityInstanceId;
    private String tenantId;
    private String userId;
    private List<HistoricDecisionInputInstance> inputs;
    private List<HistoricDecisionOutputInstance> outputs;
    private Double collectResultValue;
    private String rootDecisionInstanceId;
    private String rootProcessInstanceId;
    private String decisionRequirementsDefinitionId;
    private String decisionRequirementsDefinitionKey;
}
