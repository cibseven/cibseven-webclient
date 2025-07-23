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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Configuration properties for the CustomRestTemplate.
 * This class holds all configurable properties for HTTP client connections.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cibseven.webclient.rest")
public class RestTemplateConfiguration {

    /**
     * Whether the custom RestTemplate is enabled.
     * If false, a default RestTemplate will be used.
     */
    private boolean enabled = true;

    /**
     * Connection timeout in milliseconds.
     */
    private int connectTimeout = 5000;

    /**
     * Socket read timeout in milliseconds.
     */
    private int readTimeout = 10000;

    /**
     * Maximum number of connections per route.
     */
    private int maxConnectionsPerRoute = 20;

    /**
     * Maximum total connections.
     */
    private int maxTotalConnections = 100;

    /**
     * Connection time to live in milliseconds.
     */
    private int connectionTimeToLive = 30000;

    /**
     * Whether to enable connection pooling.
     */
    private boolean connectionPoolingEnabled = true;

    /**
     * Whether to enable connection keep-alive.
     */
    private boolean keepAliveEnabled = true;

    /**
     * Whether to enable connection reuse.
     */
    private boolean connectionReuseEnabled = true;

    /**
     * Whether to enable Micrometer metrics.
     */
    private boolean metricsEnabled = false;

    /**
     * Whether to enable request/response logging.
     */
    private boolean requestLoggingEnabled = false;

    /**
     * Whether to follow redirects.
     */
    private boolean followRedirects = true;
}