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

import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

@Getter
public class SevenDirectProvider implements BpmProvider {
    //decides about ldap/adfs
	@Value("${cibseven.webclient.user.provider:org.cibseven.webapp.auth.SevenUserProvider}") String sevenUserProvider;
	@Value("${cibseven.webclient.users.search.wildcard:}") String wildcard;
	// base project providers
	private DirectTaskProvider taskProvider = null;
	private DirectProcessProvider processProvider = null;
	private DirectFilterProvider filterProvider = null;
	private DirectDeploymentProvider deploymentProvider = null;
	private DirectVariableProvider variableProvider = null;
	private DirectActivityProvider activityProvider = null;
	private DirectIncidentProvider incidentProvider = null;
	private DirectUserProvider userProvider;
	private DirectVariableInstanceProvider variableInstanceProvider = null;
	private DirectUtilsProvider utilsProvider = null;
	private DirectHistoricVariableInstanceProvider historicVariableInstanceProvider = null;
	private DirectExternalTaskProvider externalTaskProvider = null;
	private DirectDecisionProvider decisionProvider = null;
	private DirectJobProvider jobProvider = null;
	private DirectJobDefinitionProvider jobDefinitionProvider = null;
	private DirectEngineProvider engineProvider = null;
	private DirectBatchProvider batchProvider = null;
	private DirectTenantProvider tenantProvider = null;
	private DirectSystemProvider systemProvider = null;

	DirectProviderUtil directProviderUtil = null;

	@PostConstruct
  	public void init() {
  		directProviderUtil = new DirectProviderUtil(); 
		deploymentProvider = new DirectDeploymentProvider(getDirectProviderUtil());
		variableProvider = new DirectVariableProvider(getDirectProviderUtil());
		variableInstanceProvider = new DirectVariableInstanceProvider(getDirectProviderUtil());
		historicVariableInstanceProvider = new DirectHistoricVariableInstanceProvider(getDirectProviderUtil());
		taskProvider = new DirectTaskProvider(getDirectProviderUtil());
		processProvider = new DirectProcessProvider(getDirectProviderUtil(), this);
		activityProvider = new DirectActivityProvider(getDirectProviderUtil());
		filterProvider = new DirectFilterProvider(getDirectProviderUtil());
		utilsProvider = new DirectUtilsProvider(getDirectProviderUtil());
		incidentProvider = new DirectIncidentProvider(getDirectProviderUtil());
		jobDefinitionProvider = new DirectJobDefinitionProvider(getDirectProviderUtil());
		userProvider = new DirectUserProvider(getDirectProviderUtil(), sevenUserProvider, wildcard);
		decisionProvider = new DirectDecisionProvider(getDirectProviderUtil());
		jobProvider = new DirectJobProvider(getDirectProviderUtil());
		batchProvider = new DirectBatchProvider(getDirectProviderUtil());
		systemProvider = new DirectSystemProvider(getDirectProviderUtil());
		tenantProvider = new DirectTenantProvider(getDirectProviderUtil());
		externalTaskProvider = new DirectExternalTaskProvider(getDirectProviderUtil());
		engineProvider = new DirectEngineProvider();
  	}	
}
