package org.cibseven.webapp.providers;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriBuilder;

public class DirectUriInfo implements jakarta.ws.rs.core.UriInfo{

	private MultivaluedMap<String, String> queryParameters;
	public DirectUriInfo(MultivaluedMap<String, String> queryParameters) {
		this.queryParameters = queryParameters;
	}
	
	public DirectUriInfo(Map<String, Object> filters) {
		queryParameters = new MultivaluedHashMap<>();
		if (filters != null) {
			for (Entry<String, Object> filterEntry : filters.entrySet()) {
				if (filterEntry.getValue() instanceof String) {
					queryParameters.add(filterEntry.getKey(), (String) filterEntry.getValue());
				} else if (filterEntry.getValue() instanceof List<?>) {
					for (Object valueItem : (List<?>) filterEntry.getValue()) {
						if (valueItem != null) {
							queryParameters.add(filterEntry.getKey(), valueItem.toString());
						}
					}
				}
			}
		}
	}

	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPath(boolean decode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PathSegment> getPathSegments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PathSegment> getPathSegments(boolean decode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getRequestUri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UriBuilder getRequestUriBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getAbsolutePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UriBuilder getAbsolutePathBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getBaseUri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UriBuilder getBaseUriBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MultivaluedMap<String, String> getPathParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MultivaluedMap<String, String> getPathParameters(boolean decode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MultivaluedMap<String, String> getQueryParameters() {
		return queryParameters;
	}

	@Override
	public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getMatchedURIs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getMatchedURIs(boolean decode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object> getMatchedResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI resolve(URI uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI relativize(URI uri) {
		// TODO Auto-generated method stub
		return null;
	}


}
