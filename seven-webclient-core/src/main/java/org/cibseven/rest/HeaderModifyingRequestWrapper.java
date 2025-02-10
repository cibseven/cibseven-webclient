package org.cibseven.rest;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * http://stackoverflow.com/questions/2811769/adding-an-http-header-to-the-request-in-a-servlet-filter
 */
public class HeaderModifyingRequestWrapper extends HttpServletRequestWrapper {
    
	private Map<String, String> overridingHeaders = new HashMap<>();
	
	public HeaderModifyingRequestWrapper(HttpServletRequest request, Map<String, String> overridingHeaders) {
        super(request);
        this.overridingHeaders = overridingHeaders;
    }
	
	public HeaderModifyingRequestWrapper(HttpServletRequest request, String authToken) {
        this(request, Collections.singletonMap("Authorization", authToken)); // case sensitive here !
    }    

    @Override
    public String getHeader(String name) {
        return overridingHeaders.containsKey(name) ? overridingHeaders.get(name) : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        for (String name : overridingHeaders.keySet())
            names.add(name);        
        return Collections.enumeration(names);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = Collections.list(super.getHeaders(name));
        if (overridingHeaders.containsKey(name))
            values.add(overridingHeaders.get(name));        
        return Collections.enumeration(values);
    }

}