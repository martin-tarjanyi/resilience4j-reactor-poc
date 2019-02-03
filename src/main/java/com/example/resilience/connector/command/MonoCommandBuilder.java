package com.example.resilience.connector.command;

import com.example.resilience.connector.command.decorator.CacheDecorator;
import com.example.resilience.connector.model.CacheKey;
import com.example.resilience.connector.model.Result;
import com.example.resilience.connector.serialization.Deserializer;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;

public class MonoCommandBuilder<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MonoCommandBuilder.class);

    private final ICommand command;

    private int retries = 0;
    private CircuitBreaker circuitBreaker;
    private Bulkhead bulkhead;
    private RateLimiter rateLimiter;
    private Duration timeout;
    private int cachePort;
    private boolean cacheEnabled = false;
    private Deserializer<T> deserializer;

    private MonoCommandBuilder(ICommand command)
    {
        this.command = command;
    }

    public static <T> MonoCommandBuilder<T> aBuilder(ICommand command)
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

    public MonoCommandBuilder<T> withCachePort(int cachePort)
    {
        this.cachePort = cachePort;
        return this;
    }

    public MonoCommandBuilder<T> withDeserializer(Deserializer<T> deserializer)
    {
        this.deserializer = deserializer;
        return this;
    }

    public Mono<Result<T>> build()
    {
        Mono<Result<T>> mono = command.execute().map(Result::ofRawResponse);

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
                   .transform(new CacheDecorator<>(CacheKey.valueOf(command.toString()), cachePort))
                   .map(this::deserialize)
                   .doOnError(this::handleError)
                   .onErrorResume(throwable -> Mono.just(Result.ofError(throwable)))
                   .doOnNext(r -> LOGGER.info(r.toString()))
                   .elapsed()
                   .doOnNext(this::logDuration)
                   .map(Tuple2::getT2);
    }

    private Result<T> deserialize(Result<T> rawResult)
    {
        String rawResponse = rawResult.getRawResponse();

        T deserialize = deserializer.deserialize(rawResponse);

        return rawResult.addDeserializedResponse(deserialize);
    }

    private void logDuration(Tuple2<Long, ? extends Result<?>> objects)
    {
        LOGGER.info("Command duration: " + objects.getT1() + " milliseconds");
    }

    private void handleError(Throwable t)
    {
        LOGGER.error("Command failed.", t);
    }
}
