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

import java.util.HashMap;
import java.util.Map;

import org.cibseven.bpm.engine.ProcessEngine;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SevenDirectProvider implements BpmProvider {

  //decides about ldap/adfs
	@Value("${cibseven.webclient.user.provider:org.cibseven.webapp.auth.SevenUserProvider}") String userProvider;
	@Value("${cibseven.webclient.users.search.wildcard:}") String wildcard;
	@Value("${cibseven.webclient.engineRest.url:./}") String cibsevenUrl;
	@Value("${cibseven.webclient.engineRest.path:/engine-rest}") protected String engineRestPath;

//	// TODO: used to call groupProcessStatisticsByKeyAndTenant which should be
//	// moved to some util class
//	@Autowired private IProcessProvider processProvider;

	protected Map<String, ProcessEngine> processEngines = new HashMap<>();
	protected Map<String, ObjectMapper> objectMappers = new HashMap<>();
	//protected Map<String, DirectProviderUtil> providerUtils = new HashMap<>();
	//
	DirectTaskProvider directTaskProvider = null;
	DirectProcessProvider directProcessProvider = null;
	DirectFilterProvider directFilterProvider = null;
	DirectDeploymentProvider directDeploymentProvider = null;
	DirectVariableProvider directVariableProvider = null;
	DirectActivityProvider directActivityProvider = null;
	DirectIncidentProvider directIncidentProvider = null;
	DirectUserProvider directUserProvider;
	DirectVariableInstanceProvider directVariableInstanceProvider = null;
	DirectUtilsProvider directUtilsProvider = null;
	DirectHistoricVariableInstanceProvider directHistoricVariableInstanceProvider = null;
	DirectExternalTaskProvider directExternalTaskProvider = null;
	DirectDecisionProvider directDecisionProvider = null;
	DirectJobProvider directJobProvider = null;
	DirectJobDefinitionProvider directJobDefinitionProvider = null;
	DirectEngineProvider directEngineProvider = null;
	DirectBatchProvider directBatchProvider = null;
	DirectTenantProvider directTenantProvider = null;
	DirectSystemProvider directSystemProvider = null;

	DirectProviderUtil directProviderUtil = null;
	public SevenDirectProvider() {
	}

	@PostConstruct
	public void init() {
		directProviderUtil = new DirectProviderUtil(cibsevenUrl, engineRestPath); 
	}
	protected DirectProviderUtil getDirectProviderUtil() {
		return directProviderUtil;
	}

	@Override
	public IDeploymentProvider getDeploymentProvider() {
		if (directDeploymentProvider == null)
			directDeploymentProvider = new DirectDeploymentProvider(getDirectProviderUtil());
		return directDeploymentProvider;
	}
  @Override 
  public IVariableProvider getVariableProvider() {
		if (directVariableProvider == null)
			directVariableProvider = new DirectVariableProvider(getDirectProviderUtil());
		return directVariableProvider;
	}
  @Override
	public IVariableInstanceProvider getVariableInstanceProvider() {
		if (directVariableInstanceProvider == null)
			directVariableInstanceProvider = new DirectVariableInstanceProvider(getDirectProviderUtil());
  	return directVariableInstanceProvider;
	}
  @Override
	public IHistoricVariableInstanceProvider getHistoricVariableInstanceProvider() {
		if (directHistoricVariableInstanceProvider == null)
			directHistoricVariableInstanceProvider = new DirectHistoricVariableInstanceProvider(getDirectProviderUtil());
  	return directHistoricVariableInstanceProvider;
	}
  @Override
	public ITaskProvider getTaskProvider() {
		if (directTaskProvider == null)
			directTaskProvider = new DirectTaskProvider(getDirectProviderUtil());
		return directTaskProvider;
	}
  @Override
	public IProcessProvider getProcessProvider() {
  	if (directProcessProvider == null)
  		directProcessProvider = new DirectProcessProvider(getDirectProviderUtil(), this);
  	return directProcessProvider;
	}
  @Override
	public IActivityProvider getActivityProvider() {
		if (directActivityProvider == null)
			directActivityProvider = new DirectActivityProvider(getDirectProviderUtil());
		return directActivityProvider;
	}
  @Override
	public IFilterProvider getFilterProvider() {
		if (directFilterProvider == null)
			directFilterProvider = new DirectFilterProvider(getDirectProviderUtil());
		return directFilterProvider;
	}
  @Override
	public IUtilsProvider getUtilsProvider() {
		if (directUtilsProvider == null)
			directUtilsProvider = new DirectUtilsProvider(getDirectProviderUtil());
  	return directUtilsProvider;
	}
  @Override
	public IIncidentProvider getIncidentProvider() {
		if (directIncidentProvider == null)
			directIncidentProvider = new DirectIncidentProvider(getDirectProviderUtil());
		return directIncidentProvider;
	}
  @Override
	public IJobDefinitionProvider getJobDefinitionProvider() {
		if (directJobDefinitionProvider == null)
			directJobDefinitionProvider = new DirectJobDefinitionProvider(getDirectProviderUtil());
		return directJobDefinitionProvider;
	}
  @Override
	public IUserProvider getUserProvider() {
  	if (directUserProvider == null)
  			directUserProvider = new DirectUserProvider(getDirectProviderUtil(), userProvider, wildcard);
  	return directUserProvider;
	}
  @Override
	public IDecisionProvider getDecisionProvider() {
		if (directDecisionProvider == null)
			directDecisionProvider = new DirectDecisionProvider(getDirectProviderUtil());
		return directDecisionProvider;
	}

  @Override
	public IJobProvider getJobProvider() {
		if (directJobProvider == null)
			directJobProvider = new DirectJobProvider(getDirectProviderUtil());
		return directJobProvider;
	}
  @Override
	public IBatchProvider getBatchProvider() {
		if (directBatchProvider == null)
			directBatchProvider = new DirectBatchProvider(getDirectProviderUtil());
		return directBatchProvider;
	}
  @Override
	public ISystemProvider getSystemProvider() {
		if (directSystemProvider == null)
			directSystemProvider = new DirectSystemProvider(getDirectProviderUtil());
		return directSystemProvider;
	}
  @Override
	public ITenantProvider getTenantProvider() {
		if (directTenantProvider == null)
			directTenantProvider = new DirectTenantProvider(getDirectProviderUtil());
		return directTenantProvider;
	}
  @Override
	public IExternalTaskProvider getExternalTaskProvider() {
		if (directExternalTaskProvider == null)
			directExternalTaskProvider = new DirectExternalTaskProvider(getDirectProviderUtil());
		return directExternalTaskProvider;
	}

  @Override
	public IEngineProvider getEngineProvider() {
		if (directEngineProvider == null)
			directEngineProvider = new DirectEngineProvider( );
		return directEngineProvider;
	}

}
