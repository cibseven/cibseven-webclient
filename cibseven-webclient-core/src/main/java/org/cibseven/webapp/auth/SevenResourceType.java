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
package org.cibseven.webapp.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public enum SevenResourceType {
	USER(1),
	GROUP(2),
	AUTHORIZATION(4),
	PROCESS_DEFINITION(6),
	PROCESS_INSTANCE(8),
	DECISION_DEFINITION(10),
	DECISION_REQUIREMENTS_DEFINITION(14),
	TASK(7),
	FILTER(5),
	DEPLOYMENT(9),
	CASE_DEFINITION(14),
	CASE_INSTANCE(15),
	APPLICATION(0),
    JOB_DEFINITION(17),
    BATCH(13),
    GROUP_MEMBERSHIP(3),
    HISTORIC_TASK(19),
    HISTORIC_PROCESS_INSTANCE(20),
    TENANT(11),
    TENANT_MEMBERSHIP(12),
    REPORT(15),
    DASHBOARD(17),
    USER_OPERATION_LOG_CATEGORY(17),
    SYSTEM(21),
    MESSAGE(22),
    EVENT_SUBSCRIPTION(23);
	
	private final int type;
}
