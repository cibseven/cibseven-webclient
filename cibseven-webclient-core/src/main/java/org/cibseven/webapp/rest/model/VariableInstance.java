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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

/**
 * Represents a variable instance from the Camunda API that captures
 * details about variables associated with process instances, executions, tasks, etc.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariableInstance {

    /**
     * The id of the variable instance.
     */
    private String id;

    /**
     * The name of the variable instance.
     */
    private String name;

    /**
     * The id of the process definition that this variable instance belongs to.
     */
    private String processDefinitionId;

    /**
     * The id of the process instance that this variable instance belongs to.
     */
    private String processInstanceId;

    /**
     * The id of the execution that this variable instance belongs to.
     */
    private String executionId;

    /**
     * The id of the case instance that this variable instance belongs to.
     */
    private String caseInstanceId;

    /**
     * The id of the case execution that this variable instance belongs to.
     */
    private String caseExecutionId;

    /**
     * The id of the task that this variable instance belongs to.
     */
    private String taskId;

    /**
     * The id of the batch that this variable instance belongs to.
     */
    private String batchId;

    /**
     * The id of the activity instance that this variable instance belongs to.
     */
    private String activityInstanceId;

    /**
     * The id of the tenant that this variable instance belongs to.
     */
    private String tenantId;

    /**
     * An error message in case a Java Serialized Object could not be de-serialized.
     */
    private String errorMessage;

    /**
     * Can be any value - string, number, boolean, array or object.
     * Note: Not every endpoint supports every type.
     */
    private Object value;

    /**
     * The value type of the variable.
     */
    private String type;

    /**
     * A JSON object containing additional, value-type-dependent properties.
     * For serialized variables of type Object, the following properties can be provided:
     * - objectTypeName: A string representation of the object's type name.
     * - serializationDataFormat: The serialization format used to store the variable.
     */
    private Map<String, Object> valueInfo;

	public void deserializeValue() {
		if (value == null) {
			return;
		}
		if (value instanceof String) {
			return; // already a string
		}

		if ("json".equalsIgnoreCase(type)) {
			try {
				// obj is your Java object
				ObjectMapper mapper = new ObjectMapper();

				// Convert to JsonNode (real JSON object)
				JsonNode jsonNode = mapper.valueToTree(value);

				// Now convert JsonNode to pretty or compact JSON string
				value = mapper.writeValueAsString(jsonNode);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
	}
}
