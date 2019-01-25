package com.example.resilience.connector.configuration;

import java.time.Duration;

public class EndpointConfiguration
{
    private final String name;
    private final int bulkhead;
    private final int retries;
    private final Duration timeout;
    private final RateLimitConfiguration rateLimitConfiguration;
    private final int circuitBreakerBufferSize;

    public EndpointConfiguration(String name, int bulkhead, int retries, Duration timeout,
            RateLimitConfiguration rateLimitConfiguration, int circuitBreakerBufferSize)
    {
        this.name = name;
        this.bulkhead = bulkhead;
        this.retries = retries;
        this.timeout = timeout;
        this.rateLimitConfiguration = rateLimitConfiguration;
        this.circuitBreakerBufferSize = circuitBreakerBufferSize;
    }

    public String getName()
    {
        return name;
    }

    public int getBulkhead()
    {
        return bulkhead;
    }

    public int getRetries()
    {
        return retries;
    }

    public Duration getTimeout()
    {
        return timeout;
    }

    public RateLimitConfiguration getRateLimitConfiguration()
    {
        return rateLimitConfiguration;
    }

    public int getCircuitBreakerBufferSize()
    {
        return circuitBreakerBufferSize;
    }
}
