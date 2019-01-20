package com.example.resilience.connectior.command;

import com.example.resilience.connectior.Result;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class MonoCommandBuilder<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MonoCommandBuilder.class);

    private final ICommand<T> command;

    private int retries = 0;
    private CircuitBreaker circuitBreaker;
    private Bulkhead bulkhead;
    private RateLimiter rateLimiter;
    private Duration timeout;

    private MonoCommandBuilder(ICommand<T> command)
    {
        this.command = command;
    }

    public static <T> MonoCommandBuilder<T> aBuilder(ICommand<T> command)
    {
        return new MonoCommandBuilder<>(command);
    }

    public MonoCommandBuilder<T> withCircuitBreaker(CircuitBreaker circuitBreaker)
    {
        this.circuitBreaker = circuitBreaker;
        return this;
    }

    public MonoCommandBuilder<T> withRateLimiter(RateLimiter rateLimiter)
    {
        this.rateLimiter = rateLimiter;
        return this;
    }

    public MonoCommandBuilder<T> withBulkhead(Bulkhead bulkhead)
    {
        this.bulkhead = bulkhead;
        return this;
    }

    public MonoCommandBuilder<T> withRetries(int retries)
    {
        this.retries = retries;
        return this;
    }

    public MonoCommandBuilder<T> withTimeout(Duration timeout)
    {
        this.timeout = timeout;
        return this;
    }

    public Mono<Result<T>> build()
    {
        Mono<T> mono = command.execute();

        if (circuitBreaker != null)
        {
            mono = mono.transform(CircuitBreakerOperator.of(circuitBreaker));
        }

        if (rateLimiter != null)
        {
            mono = mono.transform(RateLimiterOperator.of(rateLimiter));
        }

        if (bulkhead != null)
        {
            mono = mono.transform(BulkheadOperator.of(bulkhead));
        }


        return mono.timeout(timeout)
                   .retry(retries)
                   .doOnNext(r -> LOGGER.info(r.toString()))
                   .map(Result::ofSuccess)
                   .doOnError(this::handleError)
                   .onErrorResume(throwable -> Mono.just(Result.ofError(throwable)));
    }

    private void handleError(Throwable t)
    {
        LOGGER.error("Command failed.", t);
    }
}
