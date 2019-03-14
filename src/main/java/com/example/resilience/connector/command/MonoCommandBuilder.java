package com.example.resilience.connector.command;

import com.example.resilience.connector.command.decorator.CacheDecorator;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.logging.LogContext;
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
import reactor.util.context.Context;
import reactor.util.function.Tuple2;

import java.time.Duration;

import static com.example.resilience.connector.util.MonoOperators.doWithContext;

public final class MonoCommandBuilder<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MonoCommandBuilder.class);

    private final ICommand command;

    private int retries;
    private CircuitBreaker circuitBreaker;
    private Bulkhead bulkhead;
    private RateLimiter rateLimiter;
    private Duration timeout;
    private int cachePort;
    private Deserializer<T> deserializer;
    private boolean cacheEnabled;
    private boolean loggingEnabled;

    private MonoCommandBuilder(ICommand command)
    {
        this.command = command;
    }

    public static <T> MonoCommandBuilder<T> aBuilder(ICommand command)
    {
        return new MonoCommandBuilder<>(command);
    }

    public MonoCommandBuilder<T> withEndpointConfiguration(EndpointConfiguration configuration)
    {
        this.retries = configuration.getRetries();
        this.timeout = configuration.getTimeout();
        this.cacheEnabled = configuration.isCacheEnabled();
        this.cachePort = configuration.getCachePort();
        this.loggingEnabled = configuration.isLoggingEnabled();
        return this;
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

    public MonoCommandBuilder<T> withDeserializer(Deserializer<T> deserializer)
    {
        this.deserializer = deserializer;
        return this;
    }

    public Mono<Result<T>> build()
    {
        Mono<Result<T>> mono = command.execute()
                                      .map(Result::<T>ofRawResponse)
                                      .defaultIfEmpty(Result.empty())
                                      .timeout(timeout)
                                      .retry(retries);

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

        if (cacheEnabled)
        {
            mono = mono.transform(new CacheDecorator<>(command.generateCacheKey(), cachePort));
        }

        return mono.map(this::deserialize)
                   .onErrorResume(throwable -> Mono.just(Result.ofError(throwable)))
                   .<Result<T>>transform(doWithContext(this::log))
                   .elapsed()
                   .doOnNext(this::logDuration)
                   .map(Tuple2::getT2);
    }

    private void log(Result<?> result, Context context)
    {
        if (!loggingEnabled)
        {
            return;
        }

        LogContext logContext = context.<LogContext>getOrEmpty(LogContext.class).orElse(LogContext.create());

        logContext.add(result);

        if (!result.isSuccess())
        {
            LOGGER.error(result.toString(), result.getThrowable());
        }
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
}
