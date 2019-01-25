package com.example.resilience.connector;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.command.MonoCommandBuilder;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.configuration.RateLimitConfiguration;
import com.example.resilience.connector.model.Result;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import reactor.core.publisher.Mono;

public class Connector
{
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    Connector(CircuitBreakerRegistry circuitBreakerRegistry, BulkheadRegistry bulkheadRegistry,
            RateLimiterRegistry rateLimiterRegistry)
    {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    public Mono<Result<String>> execute(EndpointConfiguration configuration, ICommand<String> command)
    {
        String endpointName = configuration.getName();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(endpointName,
                CircuitBreakerConfig.custom()
                                    .ringBufferSizeInClosedState(configuration.getCircuitBreakerBufferSize())
                                    .build());

        Bulkhead bulkhead = bulkheadRegistry.bulkhead(endpointName, BulkheadConfig.custom().maxConcurrentCalls(configuration.getBulkhead()).build());
        RateLimiter rateLimiter = rateLimiter(endpointName, configuration.getRateLimitConfiguration());

        return MonoCommandBuilder.aBuilder(command)
                                 .withCircuitBreaker(circuitBreaker)
                                 .withBulkhead(bulkhead)
                                 .withRateLimiter(rateLimiter)
                                 .withRetries(configuration.getRetries())
                                 .withTimeout(configuration.getTimeout())
                                 .build();
    }

    private RateLimiter rateLimiter(String endpointName, RateLimitConfiguration configuration)
    {
        if (!configuration.isRateLimitEnabled())
        {
            return null;
        }

        return rateLimiterRegistry.rateLimiter(endpointName,
                RateLimiterConfig.custom()
                                 .limitForPeriod(configuration.getRateLimitPermitsPerPeriod())
                                 .limitRefreshPeriod(configuration.getRateLimitPeriod())
                                 .build());
    }
}
