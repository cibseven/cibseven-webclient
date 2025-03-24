package org.cibseven.providers;

import java.util.Collection;

import org.cibseven.auth.CIBUser;
import org.cibseven.rest.model.Filter;

public interface IFilterProvider {
	
	public Collection<Filter> findFilters(CIBUser user);
	public Filter createFilter(Filter filter, CIBUser user);
	public void updateFilter(Filter filter, CIBUser user);
	public void deleteFilter(String filterId, CIBUser user);
	
}
