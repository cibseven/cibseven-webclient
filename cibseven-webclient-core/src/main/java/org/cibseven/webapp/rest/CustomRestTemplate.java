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
package org.cibseven.webapp.rest;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Custom implementation of RestTemplate that can be configured with specific settings
 * for HTTP requests in the application.
 */
public class CustomRestTemplate extends RestTemplate {

    /**
     * Default constructor that sets up a RestTemplate with default configuration.
     */
    public CustomRestTemplate() {
        super();
        configureDefaults();
    }

    /**
     * Constructor that accepts a custom ClientHttpRequestFactory.
     *
     * @param requestFactory the request factory to use
     */
    public CustomRestTemplate(ClientHttpRequestFactory requestFactory) {
        super(requestFactory);
        configureDefaults();
    }

    /**
     * Configure default settings for this RestTemplate instance.
     */
    private void configureDefaults() {
        // Set default timeout settings
        if (getRequestFactory() instanceof SimpleClientHttpRequestFactory) {
            SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) getRequestFactory();
            factory.setConnectTimeout(5000); // 5 seconds
            factory.setReadTimeout(10000);   // 10 seconds
        }
    }
}