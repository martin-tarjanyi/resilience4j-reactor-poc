package com.example.resilience.connector.configuration.builder;

import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.configuration.RateLimitConfiguration;

import java.time.Duration;

public final class EndpointConfigurationBuilder
{
    private String name;
    private int bulkhead = 10;
    private int retries;
    private Duration timeout;
    private RateLimitConfiguration rateLimitConfiguration;
    private int circuitBreakerBufferSize;
    private int cachePort;
    private boolean cacheEnabled;

    private EndpointConfigurationBuilder()
    {
    }

    public static EndpointConfigurationBuilder aTestEndpointConfiguration()
    {
        return new EndpointConfigurationBuilder().withName("testEndpoint")
                                                 .withBulkhead(10)
                                                 .withRetries(0)
                                                 .withTimeout(Duration.ofSeconds(5))
                                                 .withRateLimitConfiguration(
                                                         new RateLimitConfiguration(false, Duration.ofSeconds(3), 3))
                                                 .withCircuitBreakerBufferSize(10)
                                                 .withCacheEnabled(false);
    }

    public static EndpointConfigurationBuilder anEndpointConfiguration()
    {
        return new EndpointConfigurationBuilder();
    }

    public EndpointConfigurationBuilder withName(String name)
    {
        this.name = name;
        return this;
    }

    public EndpointConfigurationBuilder withBulkhead(int bulkhead)
    {
        this.bulkhead = bulkhead;
        return this;
    }

    public EndpointConfigurationBuilder withRetries(int retries)
    {
        this.retries = retries;
        return this;
    }

    public EndpointConfigurationBuilder withTimeout(Duration timeout)
    {
        this.timeout = timeout;
        return this;
    }

    public EndpointConfigurationBuilder withRateLimitConfiguration(RateLimitConfiguration rateLimitConfiguration)
    {
        this.rateLimitConfiguration = rateLimitConfiguration;
        return this;
    }

    public EndpointConfigurationBuilder withCircuitBreakerBufferSize(int circuitBreakerBufferSize)
    {
        this.circuitBreakerBufferSize = circuitBreakerBufferSize;
        return this;
    }

    public EndpointConfigurationBuilder withCachePort(int cachePort)
    {
        this.cachePort = cachePort;
        return this;
    }

    public EndpointConfigurationBuilder withCacheEnabled(boolean cacheEnabled)
    {
        this.cacheEnabled = cacheEnabled;
        return this;
    }

    public EndpointConfiguration build()
    {
        return new EndpointConfiguration(name, bulkhead, retries, timeout, rateLimitConfiguration,
                circuitBreakerBufferSize, cacheEnabled, cachePort);
    }
}
