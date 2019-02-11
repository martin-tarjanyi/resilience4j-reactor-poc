package com.example.resilience.connector.configuration;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.example.resilience.connector")
public class ConnectorConfiguration
{
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry()
    {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry()
    {
        return RateLimiterRegistry.ofDefaults();
    }

    @Bean
    public BulkheadRegistry bulkheadRegistry()
    {
        return BulkheadRegistry.ofDefaults();
    }
}
