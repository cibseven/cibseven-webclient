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
