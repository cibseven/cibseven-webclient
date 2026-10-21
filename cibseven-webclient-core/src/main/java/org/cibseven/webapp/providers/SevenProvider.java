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
package org.cibseven.webapp.providers;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;

import org.cibseven.common.api.providers.BpmProvider;
import org.cibseven.common.api.providers.IActivityProvider;
import org.cibseven.common.api.providers.IBatchProvider;
import org.cibseven.common.api.providers.IDecisionProvider;
import org.cibseven.common.api.providers.IDeploymentProvider;
import org.cibseven.common.api.providers.IEngineProvider;
import org.cibseven.common.api.providers.IExternalTaskProvider;
import org.cibseven.common.api.providers.IFilterProvider;
import org.cibseven.common.api.providers.IHistoricVariableInstanceProvider;
import org.cibseven.common.api.providers.IIdentityProvider;
import org.cibseven.common.api.providers.IIncidentProvider;
import org.cibseven.common.api.providers.IJobDefinitionProvider;
import org.cibseven.common.api.providers.IJobProvider;
import org.cibseven.common.api.providers.IProcessProvider;
import org.cibseven.common.api.providers.ISystemProvider;
import org.cibseven.common.api.providers.ITaskProvider;
import org.cibseven.common.api.providers.ITenantProvider;
import org.cibseven.common.api.providers.IUserProvider;
import org.cibseven.common.api.providers.IUtilsProvider;
import org.cibseven.common.api.providers.IVariableInstanceProvider;
import org.cibseven.common.api.providers.IVariableProvider;

@Getter
public class SevenProvider extends SevenProviderBase implements BpmProvider {
    @Autowired private IDeploymentProvider deploymentProvider;
    @Autowired private IVariableProvider variableProvider;
    @Autowired private IVariableInstanceProvider variableInstanceProvider;
    @Autowired private IHistoricVariableInstanceProvider historicVariableInstanceProvider;
    @Autowired private ITaskProvider taskProvider;
    @Autowired private IProcessProvider processProvider;
    @Autowired private IActivityProvider activityProvider;
    @Autowired private IFilterProvider filterProvider;
    @Autowired private IUtilsProvider utilsProvider;
    @Autowired private IIncidentProvider incidentProvider;
    @Autowired private IJobDefinitionProvider jobDefinitionProvider;
    @Autowired private IUserProvider userProvider;
    @Autowired private IDecisionProvider decisionProvider;
    @Autowired private IJobProvider jobProvider;
    @Autowired private IBatchProvider batchProvider;
    @Autowired private ISystemProvider systemProvider;
    @Autowired private ITenantProvider tenantProvider;
    @Autowired private IExternalTaskProvider externalTaskProvider;
    @Autowired private IEngineProvider engineProvider;
	@Autowired private IIdentityProvider identityProvider;
}
