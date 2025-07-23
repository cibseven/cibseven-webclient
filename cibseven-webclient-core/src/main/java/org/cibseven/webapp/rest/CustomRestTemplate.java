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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;


/**
 * Enhanced implementation of RestTemplate that uses Apache HttpClient with connection pooling
 * and can be configured with specific settings for HTTP requests in the application.
 * 
 * Features:
 * - Connection pooling with PoolingHttpClientConnectionManager
 * - Configurable connection and read timeouts
 * - Configurable keep-alive and connection reuse strategies
 * - Support for request interceptors and custom HttpMessageConverters
 * - Optional integration with Micrometer for metrics
 */
public class CustomRestTemplate extends RestTemplate {

    private static final Logger log = LoggerFactory.getLogger(CustomRestTemplate.class);

    @Autowired(required = false)
    private RestTemplateConfiguration config;

    private Object meterRegistry;

    private List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
    private List<HttpMessageConverter<?>> customConverters = new ArrayList<>();

    // Store a reference to the connection manager for later use
    private PoolingHttpClientConnectionManager connectionManager;

    /**
     * Default constructor that sets up a RestTemplate with default configuration.
     * Configuration will be applied during @PostConstruct.
     */
    public CustomRestTemplate() {
        super();
    }

    /**
     * Constructor that accepts a custom ClientHttpRequestFactory.
     * This constructor is mainly for backward compatibility.
     *
     * @param requestFactory the request factory to use
     */
    public CustomRestTemplate(ClientHttpRequestFactory requestFactory) {
        super(requestFactory);
    }

    /**
     * Static factory method to create a fully configured CustomRestTemplate.
     * This method encapsulates all the connection/HttpClient creation logic.
     * 
     * @param config the configuration to use
     * @return a configured CustomRestTemplate instance
     */
    public static CustomRestTemplate create(RestTemplateConfiguration config) {
        CustomRestTemplate template = new CustomRestTemplate();
        template.config = config;
        template.initialize();
        return template;
    }

    /**
     * Static factory method to create a fully configured CustomRestTemplate.
     * This method encapsulates all the connection/HttpClient creation logic.
     * 
     * @param config the configuration to use
     * @param meterRegistry optional meter registry for metrics
     * @return a configured CustomRestTemplate instance
     */
    public static CustomRestTemplate create(RestTemplateConfiguration config, Object meterRegistry) {
        CustomRestTemplate template = new CustomRestTemplate();
        template.config = config;
        template.meterRegistry = meterRegistry;
        template.initialize();
        return template;
    }

    /**
     * Add a request interceptor to the RestTemplate.
     * 
     * @param interceptor the interceptor to add
     * @return this CustomRestTemplate instance for method chaining
     */
    public CustomRestTemplate addInterceptor(ClientHttpRequestInterceptor interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    /**
     * Add a custom message converter to the RestTemplate.
     * 
     * @param converter the converter to add
     * @return this CustomRestTemplate instance for method chaining
     */
    public CustomRestTemplate addConverter(HttpMessageConverter<?> converter) {
        this.customConverters.add(converter);
        return this;
    }

    /**
     * Initialize the RestTemplate with the configured settings.
     * This method is called automatically by Spring after dependency injection.
     */
    @PostConstruct
    public void initialize() {
        if (config == null) {
            log.info("No RestTemplateConfiguration found, using default settings");
            config = new RestTemplateConfiguration();
        }

        if (!config.isEnabled()) {
            log.info("CustomRestTemplate is disabled, using default RestTemplate configuration");
            return;
        }

        log.info("Initializing CustomRestTemplate with connection pooling");

        // Log the configuration details
        log.info("RestTemplateConfiguration: enabled={}, connectTimeout={}ms, readTimeout={}ms, maxConnectionsPerRoute={}, maxTotalConnections={}, " +
                "connectionTimeToLive={}ms, connectionPoolingEnabled={}, keepAliveEnabled={}, connectionReuseEnabled={}, " +
                "metricsEnabled={}, requestLoggingEnabled={}, followRedirects={}",
                config.isEnabled(), config.getConnectTimeout(), config.getReadTimeout(), config.getMaxConnectionsPerRoute(),
                config.getMaxTotalConnections(), config.getConnectionTimeToLive(), config.isConnectionPoolingEnabled(),
                config.isKeepAliveEnabled(), config.isConnectionReuseEnabled(), config.isMetricsEnabled(),
                config.isRequestLoggingEnabled(), config.isFollowRedirects());

        // Create and configure the HttpClient
        CloseableHttpClient httpClient = createHttpClient();

        // Create and set the request factory
        HttpComponentsClientHttpRequestFactory requestFactory = 
                new HttpComponentsClientHttpRequestFactory(httpClient);

        setRequestFactory(requestFactory);

        // Apply interceptors if any
        if (!interceptors.isEmpty()) {
            setInterceptors(interceptors);
        }

        // Apply custom converters if any
        if (!customConverters.isEmpty()) {
            customConverters.forEach(this::addCustomConverter);
        }

        // Register with Micrometer if enabled and available
        if (config.isMetricsEnabled() && meterRegistry != null) {
            registerWithMicrometer();
        }
    }

    /**
     * Create and configure an Apache HttpClient with the settings from the configuration.
     * 
     * @return a configured CloseableHttpClient
     */
    private CloseableHttpClient createHttpClient() {
        // Create connection manager
        PoolingHttpClientConnectionManager connectionManager = createConnectionManager();

        // Create request config
        RequestConfig requestConfig = createRequestConfig();

        // Create keep-alive strategy
        ConnectionKeepAliveStrategy keepAliveStrategy = createKeepAliveStrategy();

        // Create default headers
        List<Header> defaultHeaders = createDefaultHeaders();

        // Build the HttpClient
        HttpClientBuilder clientBuilder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setDefaultHeaders(defaultHeaders);

        // Apply keep-alive strategy if enabled
        if (config.isKeepAliveEnabled()) {
            clientBuilder.setKeepAliveStrategy(keepAliveStrategy);
        }

        // Apply retry strategy (default: no retries)
        clientBuilder.setRetryStrategy(new DefaultHttpRequestRetryStrategy(0, TimeValue.ZERO_MILLISECONDS));

        // Apply connection reuse strategy if enabled
        if (config.isConnectionReuseEnabled()) {
            clientBuilder.setConnectionReuseStrategy((request, response, context) -> true);
        }

        // Set whether to follow redirects
        if (!config.isFollowRedirects()) {
            // Disable redirect handling if redirects are not allowed
            clientBuilder.disableRedirectHandling();
        }
        // Default behavior is to follow redirects, so we don't need to set anything if redirects are enabled

        return clientBuilder.build();
    }

    /**
     * Create and configure a PoolingHttpClientConnectionManager.
     * 
     * @return a configured PoolingHttpClientConnectionManager
     */
    private PoolingHttpClientConnectionManager createConnectionManager() {
        // Create the connection manager and store it in the class field for later use
        this.connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnPerRoute(config.getMaxConnectionsPerRoute())
                .setMaxConnTotal(config.getMaxTotalConnections())
                .setConnectionTimeToLive(TimeValue.of(config.getConnectionTimeToLive(), TimeUnit.MILLISECONDS))
                .build();

        return this.connectionManager;
    }

    /**
     * Create and configure a RequestConfig.
     * 
     * @return a configured RequestConfig
     */
    private RequestConfig createRequestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(Timeout.of(config.getConnectTimeout(), TimeUnit.MILLISECONDS))
                .setResponseTimeout(Timeout.of(config.getReadTimeout(), TimeUnit.MILLISECONDS))
                .build();
    }

    /**
     * Create a ConnectionKeepAliveStrategy.
     * 
     * @return a ConnectionKeepAliveStrategy
     */
    private ConnectionKeepAliveStrategy createKeepAliveStrategy() {
        return new DefaultConnectionKeepAliveStrategy() {
            @Override
            public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
                TimeValue duration = super.getKeepAliveDuration(response, context);
                if (duration.getDuration() <= 0) {
                    // If no keep-alive header is present, use a default value
                    return TimeValue.of(30, TimeUnit.SECONDS);
                }
                return duration;
            }
        };
    }

    /**
     * Create default headers for all requests.
     * 
     * @return a list of default headers
     */
    private List<Header> createDefaultHeaders() {
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("User-Agent", "CibSeven-CustomRestTemplate"));
        return headers;
    }

    /**
     * Add a custom converter to the RestTemplate.
     * 
     * @param converter the converter to add
     */
    private void addCustomConverter(HttpMessageConverter<?> converter) {
        List<HttpMessageConverter<?>> converters = getMessageConverters();
        converters.add(0, converter); // Add at the beginning to give it higher priority
        setMessageConverters(converters);
    }

    /**
     * Register the RestTemplate with Micrometer for metrics collection.
     * This method uses reflection to avoid direct dependencies on Micrometer classes,
     * which allows the application to start even if Micrometer is not available.
     */
    private void registerWithMicrometer() {
        try {
            // Try to load the MeterRegistry class
            Class<?> meterRegistryClass = Class.forName("io.micrometer.core.instrument.MeterRegistry");

            // If meterRegistry is null or not an instance of MeterRegistry, return
            if (meterRegistry == null || !meterRegistryClass.isInstance(meterRegistry)) {
                return;
            }

            log.info("Registering CustomRestTemplate with Micrometer");

            // Get the request factory
            HttpComponentsClientHttpRequestFactory factory = 
                    (HttpComponentsClientHttpRequestFactory) getRequestFactory();

            // Get the gauge method from MeterRegistry
            java.lang.reflect.Method gaugeMethod = meterRegistryClass.getMethod("gauge", 
                    String.class, Object.class, java.util.function.ToDoubleFunction.class);

            // Register metrics for connection pool
            gaugeMethod.invoke(meterRegistry, "http.client.connections.max", 
                    config, (java.util.function.ToDoubleFunction<RestTemplateConfiguration>) 
                    c -> c.getMaxTotalConnections());

            gaugeMethod.invoke(meterRegistry, "http.client.connections.max.per.route", 
                    config, (java.util.function.ToDoubleFunction<RestTemplateConfiguration>) 
                    c -> c.getMaxConnectionsPerRoute());

        } catch (ClassNotFoundException e) {
            // MeterRegistry class not found, which is fine if Micrometer is not available
            log.debug("Micrometer not available, skipping metrics registration");
        } catch (Exception e) {
            // Log any other exceptions but don't fail
            log.warn("Failed to register metrics for connection pool", e);
        }
    }

    /**
     * Clean up resources when the application is shutting down.
     */
    public void destroy() {
        try {
            if (getRequestFactory() instanceof HttpComponentsClientHttpRequestFactory) {
                HttpComponentsClientHttpRequestFactory factory = 
                        (HttpComponentsClientHttpRequestFactory) getRequestFactory();
                factory.destroy();
            }
        } catch (Exception e) {
            log.warn("Error destroying HttpComponentsClientHttpRequestFactory", e);
        }
    }

    /**
     * Get the connection pool statistics.
     * 
     * @return a string representation of the connection pool statistics
     */
    public String getConnectionPoolStats() {
        try {
            // Use the stored connection manager reference instead of trying to access it through reflection
            if (this.connectionManager != null) {
                // Return the connection pool statistics
                return String.format("MaxTotal: %d, DefaultMaxPerRoute: %d, Available: %d, Leased: %d, Pending: %d",
                        connectionManager.getMaxTotal(),
                        connectionManager.getDefaultMaxPerRoute(),
                        connectionManager.getTotalStats().getAvailable(),
                        connectionManager.getTotalStats().getLeased(),
                        connectionManager.getTotalStats().getPending());
            }
            return "Connection pool statistics not available (connection manager not initialized)";
        } catch (Exception e) {
            log.warn("Error getting connection pool statistics", e);
            return "Error getting connection pool statistics: " + e.getMessage();
        }
    }
}
