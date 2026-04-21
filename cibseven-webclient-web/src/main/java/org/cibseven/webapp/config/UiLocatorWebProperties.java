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
package org.cibseven.webapp.config;

import org.cibseven.templates.uilocator.ExternalUiTask;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "euit")
public class UiLocatorWebProperties {

	private String engineRestPath = "";
	
	private List<ExternalUiTask> tasks = new ArrayList<>();

	public String getEngineRestUrl() {
		return engineRestPath;
	}

	public void setEngineRestUrl(String engineRestUrl) {
		this.engineRestPath = engineRestUrl;
	}

	public String getEngineRestPath() {
		return engineRestPath;
	}

	public void setEngineRestPath(String engineRestPath) {
		this.engineRestPath = engineRestPath;
	}

	public List<ExternalUiTask> getTasks() {
		return tasks;
	}

	public void setTasks(List<ExternalUiTask> tasks) {
		this.tasks = tasks;
	}
}