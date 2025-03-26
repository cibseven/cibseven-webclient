package org.cibseven.webapp.providers;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Decision;

public interface IDecisionProvider {
	
	public Collection<Decision> getDecisionDefinitionList(Map<String, Object> queryParams);
	public Object getDecisionDefinitionListCount(Map<String, Object> queryParams);
	public Decision getDecisionDefinitionByKey(String key);
	public Object getDiagramByKey(String key);
	public Object evaluateDecisionDefinitionByKey(Map<String, Object> data, String key, CIBUser user);
	public void updateHistoryTTLByKey(Map<String, Object> data, String key, CIBUser user);
	public Decision getDecisionDefinitionByKeyAndTenant(String key, String tenant);
	public Object getDiagramByKeyAndTenant(String key, String tenant);
	public Object evaluateDecisionDefinitionByKeyAndTenant(String key, String tenant);
	public Object updateHistoryTTLByKeyAndTenant(String key, String tenant);
	public Object getXmlByKey(String key);
	public Object getXmlByKeyAndTenant(String key, String tenant);
	public Decision getDecisionDefinitionById(String id);
	public Object getDiagramById(String id);
	public Object evaluateDecisionDefinitionById(String id);
	public Object updateHistoryTTLById(String id);
	public Object getXmlById(String id);
	public Collection<Decision> getDecisionVersionsByKey(String key, Optional<Boolean> lazyLoad);
}
