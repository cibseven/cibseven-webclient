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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an external task from the Camunda API that captures
 * details about external tasks that are currently available for processing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalTask {

  /**
   * The id of the external task.
   */
  private String id;

  /**
   * The id of the activity that this external task belongs to.
   */
  private String activityId;

  /**
   * The id of the activity instance that the external task belongs to.
   */
  private String activityInstanceId;

  /**
   * The full error message submitted with the latest reported failure executing this task;
   * null if no failure was reported previously or if no error message was submitted.
   */
  private String errorMessage;

  /**
   * The id of the execution that the external task belongs to.
   */
  private String executionId;

  /**
   * The date that the task's most recent lock expires or has expired.
   */
  private String lockExpirationTime;

  /**
   * The id of the process definition the external task is defined in.
   */
  private String processDefinitionId;

  /**
   * The key of the process definition the external task is defined in.
   */
  private String processDefinitionKey;

  /**
   * The version tag of the process definition the external task is defined in.
   */
  private String processDefinitionVersionTag;

  /**
   * The id of the process instance the external task belongs to.
   */
  private String processInstanceId;

  /**
   * The id of the tenant the external task belongs to.
   */
  private String tenantId;

  /**
   * The number of retries the task currently has left.
   */
  private Integer retries;

  /**
   * A flag indicating whether the external task is suspended or not.
   */
  private Boolean suspended;

  /**
   * The id of the worker that possesses or possessed the most recent lock.
   */
  private String workerId;

  /**
   * The topic name of the external task.
   */
  private String topicName;

  /**
   * The priority of the external task.
   */
  private Long priority;

  /**
   * The business key of the process instance the external task belongs to.
   */
  private String businessKey;
}