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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import lombok.Data;

/**
 * Configuration properties for the CustomRestTemplate.
 * This class holds all configurable properties for HTTP client connections.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cibseven.webclient.rest")
@ConditionalOnProperty(
        prefix = "cibseven.webclient.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RestTemplateConfiguration {

    /**
     * Whether the custom RestTemplate is enabled.
     * If false, a default RestTemplate will be used.
     */
    private boolean enabled = true;

    /**
     * Whether the socket connection should be kept alive.
     */
    private boolean keepAlive = true;

    /**
     * Socket read timeout in seconds (0 = infinite).
     */
    private int socketTimeout = 3600;

    /**
     * Time in seconds to establish connection (0 = infinite).
     */
    private int connectTimeout = 30;

    /**
     * Time to wait for a connection from the pool in seconds.
     */
    private int connectionRequestTimeout = 180;

    /**
     * Max response wait time in seconds (0 = infinite).
     * If null, the socketTimeout value will be used.
     */
    private Integer connectionResponseTimeout = null;

    /**
     * Max HTTP connections per route.
     */
    private int maxConnPerRoute = 20;

    /**
     * Total max HTTP connections.
     * Defaults to 2 * maxConnPerRoute.
     */
    private int maxConnTotal = 40;

    /**
     * Connection time to live in milliseconds.
     */
    private int connectionTimeToLive = 30000;

    /**
     * Whether to enable connection pooling.
     */
    private boolean connectionPoolingEnabled = true;

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

    /**
     * Get the maximum total connections.
     * If not explicitly set, returns 2 * maxConnPerRoute.
     * 
     * @return the maximum total connections
     */
    public int getMaxConnTotal() {
        return maxConnTotal == 0 ? maxConnPerRoute * 2 : maxConnTotal;
    }

    /**
     * Provides a string representation of the configuration for logging purposes.
     * 
     * @return a string representation of the configuration
     */
    @Override
    public String toString() {
        return "RestTemplateConfiguration{" +
                "enabled=" + enabled +
                ", keepAlive=" + keepAlive +
                ", socketTimeout=" + socketTimeout +
                ", connectTimeout=" + connectTimeout +
                ", connectionRequestTimeout=" + connectionRequestTimeout +
                ", connectionResponseTimeout=" + connectionResponseTimeout +
                ", maxConnPerRoute=" + maxConnPerRoute +
                ", maxConnTotal=" + maxConnTotal +
                ", connectionTimeToLive=" + connectionTimeToLive +
                ", connectionPoolingEnabled=" + connectionPoolingEnabled +
                ", connectionReuseEnabled=" + connectionReuseEnabled +
                ", metricsEnabled=" + metricsEnabled +
                ", requestLoggingEnabled=" + requestLoggingEnabled +
                ", followRedirects=" + followRedirects +
                '}';
    }
}
