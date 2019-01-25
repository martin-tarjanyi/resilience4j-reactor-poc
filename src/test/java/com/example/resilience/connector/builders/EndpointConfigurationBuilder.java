package com.example.resilience.connector.builders;

import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.configuration.RateLimitConfiguration;

import java.time.Duration;

public final class EndpointConfigurationBuilder
{
    private String name = "testEndpoint";
    private int bulkhead = 10;
    private int retries = 0;
    private Duration timeout = Duration.ofSeconds(5);
    private RateLimitConfiguration rateLimitConfiguration = new RateLimitConfiguration(false, Duration.ofSeconds(3), 3);
    private int circuitBreakerBufferSize = 10;

    private EndpointConfigurationBuilder()
    {
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

    public EndpointConfiguration build()
    {
        return new EndpointConfiguration(name, bulkhead, retries, timeout, rateLimitConfiguration,
                circuitBreakerBufferSize);
    }
}
