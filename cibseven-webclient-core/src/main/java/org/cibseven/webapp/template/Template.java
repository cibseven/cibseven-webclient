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
package org.cibseven.webapp.template;

import java.util.List;
import java.util.Map;

import org.cibseven.webapp.rest.model.ActivityInstance;
import org.cibseven.webapp.rest.model.ActivityInstanceHistory;
import org.cibseven.webapp.rest.model.ProcessDiagram;
import org.cibseven.webapp.rest.model.Task;
import org.cibseven.webapp.rest.model.Variable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

@Data
public class Template {
	private Map<String, Variable> variables;
	private ProcessDiagram bpmDiagram;
	private ActivityInstance activityInstances;
	private List<ActivityInstanceHistory> activityInstanceHistory;
	private Task task;

	public String asJson() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(this);
	}			
}
