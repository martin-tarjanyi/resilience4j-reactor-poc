package com.example.resilience.connector;

import com.example.resilience.connector.command.MonoCommandBuilder;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.configuration.RateLimitConfiguration;
import com.example.resilience.connector.model.CommandDescriptor;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

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

    public <T> Result<T> executeBlocking(CommandDescriptor<T> commandDescriptor)
    {
        return execute(commandDescriptor).block();
    }

    public <T> Mono<Result<T>> execute(CommandDescriptor<T> commandDescriptor)
    {
        EndpointConfiguration configuration = commandDescriptor.getEndpointConfiguration();
        String endpointName = configuration.getName();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(endpointName,
                CircuitBreakerConfig.custom()
                                    .ringBufferSizeInClosedState(configuration.getCircuitBreakerBufferSize())
                                    .build());

        Bulkhead bulkhead = bulkheadRegistry.bulkhead(endpointName, BulkheadConfig.custom().maxConcurrentCalls(configuration.getBulkhead()).build());
        RateLimiter rateLimiter = rateLimiter(endpointName, configuration.getRateLimitConfiguration());

        return MonoCommandBuilder.<T>aBuilder(commandDescriptor.getCommand())
                .withCircuitBreaker(circuitBreaker)
                .withBulkhead(bulkhead)
                .withRateLimiter(rateLimiter)
                .withRetries(configuration.getRetries())
                .withTimeout(configuration.getTimeout())
                .withCachePort(configuration.getCachePort())
                .withDeserializer(commandDescriptor.getDeserializer())
                .build();
    }

    public <T> List<Result<T>> executeBlocking(Collection<? extends CommandDescriptor<T>> commandDescriptors)
    {
        return execute(commandDescriptors).collectList().block();
    }

    public <T> Flux<Result<T>> execute(Collection<? extends CommandDescriptor<T>> commandDescriptors)
    {
        return Flux.fromIterable(commandDescriptors)
                   .flatMap(this::execute, commandDescriptors.size());
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
