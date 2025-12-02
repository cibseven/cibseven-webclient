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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Decision;
import org.cibseven.webapp.rest.model.HistoricDecisionInstance;

public interface IDecisionProvider {
	
	public Collection<Decision> getDecisionDefinitionList(Map<String, Object> queryParams, CIBUser user);
	public Long getDecisionDefinitionListCount(Map<String, Object> queryParams, CIBUser user);
	public Decision getDecisionDefinitionByKey(String key, CIBUser user);
	public Object getDiagramByKey(String key, CIBUser user);
	public Object evaluateDecisionDefinitionByKey(Map<String, Object> data, String key, CIBUser user);
	public void updateHistoryTTLByKey(Map<String, Object> data, String key, CIBUser user);

	public Decision getDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user);
	public Object getDiagramByKeyAndTenant(String key, String tenant, CIBUser user);
	public Object evaluateDecisionDefinitionByKeyAndTenant(Map<String, Object> data, String key, String tenant, CIBUser user);
	public Object updateHistoryTTLByKeyAndTenant(String key, String tenant, CIBUser user);
	public Object getXmlByKey(String key, CIBUser user);
	public Object getXmlByKeyAndTenant(String key, String tenant, CIBUser user);
	public Decision getDecisionDefinitionById(String id, Optional<Boolean> extraInfo, CIBUser user);
	public Object getDiagramById(String id, CIBUser user);
	public Object evaluateDecisionDefinitionById(String id, CIBUser user);
	public void updateHistoryTTLById(String id, Map<String, Object> data, CIBUser user);
	public Object getXmlById(String id, CIBUser user);
	public Collection<Decision> getDecisionVersionsByKey(String key, Optional<Boolean> lazyLoad, CIBUser user);
	public Collection<HistoricDecisionInstance> getHistoricDecisionInstances(Map<String, Object> queryParams, CIBUser user);
	public Long getHistoricDecisionInstanceCount(Map<String, Object> queryParams, CIBUser user);
	public HistoricDecisionInstance getHistoricDecisionInstanceById(String id, Map<String, Object> queryParams, CIBUser user);
	public Object deleteHistoricDecisionInstances(Map<String, Object> body, CIBUser user);
	public Object setHistoricDecisionInstanceRemovalTime(Map<String, Object> body, CIBUser user);

}
