# CustomRestTemplate Implementation

## Overview

This branch introduces a new `CustomRestTemplate` implementation that enhances Spring's standard `RestTemplate` with advanced features for HTTP client operations. The implementation provides better performance, reliability, and configurability for all HTTP requests made from the CIB seven webclient application.

## What's New

### Core Components

1. **CustomRestTemplate**
   - Enhanced implementation of Spring's RestTemplate with connection pooling
   - Configurable timeouts, connection limits, and retry strategies
   - Support for metrics collection via Micrometer (optional)
   - Connection pool statistics for monitoring
   - Thread-safe implementation for concurrent environments

2. **RestTemplateConfiguration**
   - Configuration properties class with sensible defaults
   - Fully configurable via application.yaml
   - Prefix: `cibseven.webclient.rest`

3. **Integration**
   - Bean definition in SevenWebclientContext
   - Usage in provider classes (SevenProviderBase and others)
   - System monitoring endpoint for connection pool statistics

## Benefits

- **Improved Performance**: Connection pooling reduces the overhead of creating new connections for each request
- **Better Reliability**: Configurable timeouts and retry strategies help handle network issues gracefully
- **Resource Management**: Connection limits prevent resource exhaustion under high load
- **Monitoring**: Connection pool statistics and optional Micrometer integration for metrics
- **Configurability**: All aspects can be configured via application.yaml without code changes

## Configuration Options

The following configuration options are available under the `cibseven.webclient.rest` prefix in application.yaml:

| Property | Description | Default |
|----------|-------------|---------|
| enabled | Whether to use the custom RestTemplate | true |
| keepAlive | Whether to keep socket connections alive | true |
| socketTimeout | Socket read timeout in seconds (0 = infinite) | 3600 |
| connectTimeout | Connection establishment timeout in seconds | 30 |
| connectionRequestTimeout | Time to wait for a connection from the pool in seconds | 180 |
| connectionResponseTimeout | Max response wait time in seconds (null = use socketTimeout) | null |
| maxConnPerRoute | Max HTTP connections per route | 20 |
| maxConnTotal | Total max HTTP connections | 40 |
| connectionTimeToLive | Connection time to live in milliseconds | 50000 |
| connectionPoolingEnabled | Whether to enable connection pooling | true |
| connectionReuseEnabled | Whether to enable connection reuse | true |
| metricsEnabled | Whether to enable Micrometer metrics | false |
| requestLoggingEnabled | Whether to enable request/response logging | false |
| followRedirects | Whether to follow redirects | true |

## Usage Examples

### Basic Usage

The `CustomRestTemplate` is automatically configured and available for injection:

```java
@Autowired
private CustomRestTemplate restTemplate;

// Use like a standard RestTemplate
ResponseEntity<String> response = restTemplate.getForEntity("https://example.com/api", String.class);
```

### Monitoring Connection Pool

The SystemService exposes an endpoint to monitor the connection pool:

```
GET /services/v1/system/pool-status
```

This returns statistics about the connection pool, including:
- Maximum total connections
- Maximum connections per route
- Available connections
- Leased connections
- Pending connections

### Thread-Safety Considerations

When using the `CustomRestTemplate` in multi-threaded environments, be aware that modifying the message converters list is not thread-safe. The `SevenProviderBase` class demonstrates a pattern for safely using the `CustomRestTemplate` in such environments by creating a new instance with copied converters:

```java
// Create a new RestTemplate instance
RestTemplate newTemplate = new RestTemplate();

// Copy converters from the shared CustomRestTemplate
newTemplate.setMessageConverters(new ArrayList<>(customRestTemplate.getMessageConverters()));

// Add custom converters if needed
newTemplate.getMessageConverters().add(new CustomConverter());

// Use the new template for this specific request
return newTemplate.exchange(uri, method, entity, responseType);
```

## Configuration Examples

### Default Configuration

The default configuration in application.yaml provides a good balance of performance and reliability:

```yaml
cibseven:
  webclient:
    rest:
      enabled: true
      keepAlive: true
      socketTimeout: 3600
      connectTimeout: 30
      connectionRequestTimeout: 180
      maxConnPerRoute: 20
      maxConnTotal: 40
      connectionTimeToLive: 50000
      connectionPoolingEnabled: true
      connectionReuseEnabled: true
      metricsEnabled: false
      requestLoggingEnabled: false
      followRedirects: true
```

### High-Performance Configuration

For high-throughput scenarios:

```yaml
cibseven:
  webclient:
    rest:
      maxConnPerRoute: 50
      maxConnTotal: 100
      connectionTimeToLive: 120000
      metricsEnabled: true  # Enable metrics for monitoring
```

### Debugging Configuration

For troubleshooting:

```yaml
cibseven:
  webclient:
    rest:
      requestLoggingEnabled: true
      socketTimeout: 60  # Shorter timeout to quickly identify slow responses
      connectTimeout: 10
```

## Conclusion

The `CustomRestTemplate` implementation provides a robust, configurable, and performant HTTP client for the CIB seven webclient application. It addresses common issues with Spring's standard RestTemplate, such as lack of connection pooling and limited configurability, while maintaining compatibility with the RestTemplate API.