package org.cibseven.webapp.providers;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.exception.SystemException;
import org.cibseven.webapp.rest.model.EventSubscription;
import org.cibseven.webapp.rest.model.Message;

public interface IUtilsProvider {

	public Collection<Message> correlateMessage(Map<String, Object> data, CIBUser user) throws SystemException;
	public String findStacktrace(String jobId, CIBUser user);
	public void retryJobById(String jobId, Map<String, Object> data, CIBUser user);
	public Collection<EventSubscription> getEventSubscriptions(Optional<String> processInstanceId,
			Optional<String> eventType, Optional<String> eventName, CIBUser user);
	
}
