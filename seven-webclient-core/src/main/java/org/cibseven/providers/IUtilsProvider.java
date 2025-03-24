package org.cibseven.providers;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.cibseven.auth.CIBUser;
import org.cibseven.exception.SystemException;
import org.cibseven.rest.model.EventSubscription;
import org.cibseven.rest.model.Message;

public interface IUtilsProvider {

	public Collection<Message> correlateMessage(Map<String, Object> data, CIBUser user) throws SystemException;
	public String findStacktrace(String jobId, CIBUser user);
	public void retryJobById(String jobId, Map<String, Object> data, CIBUser user);
	public Collection<EventSubscription> getEventSubscriptions(Optional<String> processInstanceId,
			Optional<String> eventType, Optional<String> eventName, CIBUser user);
	
}
