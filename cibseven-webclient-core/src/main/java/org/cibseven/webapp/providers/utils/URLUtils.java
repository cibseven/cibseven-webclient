package org.cibseven.webapp.providers.utils;

import java.util.Map;
import lombok.experimental.UtilityClass;

import org.springframework.web.util.UriComponentsBuilder;

@UtilityClass
public class URLUtils {

	public static String buildUrlWithParams(String baseUrl, Map<String, Object> queryParams) {
	    if (queryParams.isEmpty()) return baseUrl; 
	    	
	    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);	    	
	    queryParams.forEach((key, value) -> {
	        if (value != null) {
	            builder.queryParam(key, value);
	        }
	    });
	    return builder.toUriString();
	}
}
