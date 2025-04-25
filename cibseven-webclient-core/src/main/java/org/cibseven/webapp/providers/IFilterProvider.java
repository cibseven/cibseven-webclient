package org.cibseven.webapp.providers;

import java.util.Collection;

import org.cibseven.webapp.auth.CIBUser;
import org.cibseven.webapp.rest.model.Filter;

public interface IFilterProvider {
	
	public Collection<Filter> findFilters(CIBUser user);
	public Filter createFilter(Filter filter, CIBUser user);
	public void updateFilter(Filter filter, CIBUser user);
	public void deleteFilter(String filterId, CIBUser user);
	
}
