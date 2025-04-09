package org.cibseven.webapp.providers;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Decision;

public interface IDecisionProvider {
	
	public Collection<Decision> getDecisionDefinitionList(Map<String, Object> queryParams, CIBUser user);
	public Object getDecisionDefinitionListCount(Map<String, Object> queryParams, CIBUser user);
	public Decision getDecisionDefinitionByKey(String key, CIBUser user);
	public Object getDiagramByKey(String key, CIBUser user);
	public Object evaluateDecisionDefinitionByKey(Map<String, Object> data, String key, CIBUser user);
	public void updateHistoryTTLByKey(Map<String, Object> data, String key, CIBUser user);

	public Decision getDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user);
	public Object getDiagramByKeyAndTenant(String key, String tenant, CIBUser user);
	public Object evaluateDecisionDefinitionByKeyAndTenant(String key, String tenant, CIBUser user);
	public Object updateHistoryTTLByKeyAndTenant(String key, String tenant, CIBUser user);
	public Object getXmlByKey(String key, CIBUser user);
	public Object getXmlByKeyAndTenant(String key, String tenant, CIBUser user);
	public Decision getDecisionDefinitionById(String id, Optional<Boolean> extraInfo);
	public Object getDiagramById(String id, CIBUser user);
	public Object evaluateDecisionDefinitionById(String id, CIBUser user);
	public void updateHistoryTTLById(String id, Map<String, Object> data, CIBUser user);
	public Object getXmlById(String id, CIBUser user);
	public Collection<Decision> getDecisionVersionsByKey(String key, Optional<Boolean> lazyLoad, CIBUser user);
	public Object getHistoricDecisionInstances(Map<String, Object> queryParams, CIBUser user);
	public Object getHistoricDecisionInstanceCount(Map<String, Object> queryParams, CIBUser user);
	public Object getHistoricDecisionInstanceById(String id, Map<String, Object> queryParams, CIBUser user);
	public Object deleteHistoricDecisionInstances(Map<String, Object> body, CIBUser user);
	public Object setHistoricDecisionInstanceRemovalTime(Map<String, Object> body, CIBUser user);

}
