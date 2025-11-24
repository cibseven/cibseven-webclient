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
    

		@Override
		public IDeploymentProvider getDeploymentProvider() {
  		return deploymentProvider;
  	}
    @Override 
    public IVariableProvider getVariableProvider() {
  		return variableProvider;
  	}
    @Override 
    public IVariableInstanceProvider getVariableInstanceProvider() {
  		return variableInstanceProvider;
  	}
    @Override 
    public IHistoricVariableInstanceProvider getHistoricVariableInstanceProvider() {
  		return historicVariableInstanceProvider;
  	}
    @Override 
    public ITaskProvider getTaskProvider() {
  		return taskProvider;
  	}
    @Override 
    public IProcessProvider getProcessProvider() {
  		return processProvider;
  	}
    @Override 
    public IActivityProvider getActivityProvider() {
  		return activityProvider;
  	}
    @Override 
    public IFilterProvider getFilterProvider() {
  		return filterProvider;
  	}
    @Override 
    public IUtilsProvider getUtilsProvider() {
  		return utilsProvider;
  	}
    @Override 
    public IIncidentProvider getIncidentProvider() {
  		return incidentProvider;
  	}
    @Override 
    public IJobDefinitionProvider getJobDefinitionProvider() {
  		return jobDefinitionProvider;
  	}
    @Override 
    public IUserProvider getUserProvider() {
  		return userProvider;
  	}
    @Override 
    public IDecisionProvider getDecisionProvider() {
  		return decisionProvider;
  	}
    @Override 
    public IJobProvider getJobProvider() {
  		return jobProvider;
  	}
    @Override 
    public IBatchProvider getBatchProvider() {
  		return batchProvider;
  	}
    @Override 
    public ISystemProvider getSystemProvider() {
  		return systemProvider;
  	}
    @Override 
    public ITenantProvider getTenantProvider() {
  		return tenantProvider;
  	}
    @Override 
    public IExternalTaskProvider getExternalTaskProvider() {
  		return externalTaskProvider;
  	}
    @Override 
    public IEngineProvider getEngineProvider() {
  		return engineProvider;
  	}
    
}
