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

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class Authorizations {
	private Collection<Authorization> application;
	private Collection<Authorization> processDefinition;
	private Collection<Authorization> processInstance;
	private Collection<Authorization> task;
	private Collection<Authorization> filter;
	private Collection<Authorization> authorization;
	private Collection<Authorization> user;
	private Collection<Authorization> group;
	private Collection<Authorization> decisionDefinition;
	private Collection<Authorization> decisionRequirementsDefinition;
	private Collection<Authorization> history;
	private Collection<Authorization> deployment;
	private Collection<Authorization> caseDefinition;
	private Collection<Authorization> caseInstance;
	private Collection<Authorization> jobDefinition;
	private Collection<Authorization> batch;
	private Collection<Authorization> groupMembership;
	private Collection<Authorization> historicTask;
	private Collection<Authorization> historicProcessInstance;
	private Collection<Authorization> tenant;
	private Collection<Authorization> tenantMembership;
	private Collection<Authorization> report;
	private Collection<Authorization> dashboard;
	private Collection<Authorization> userOperationLogCategory;
	private Collection<Authorization> system;
	private Collection<Authorization> message;
	private Collection<Authorization> eventSubscription;
}
