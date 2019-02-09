package com.example.resilience.connector.command.decorator;

import com.example.resilience.connector.command.MonoCommandBuilder;
import com.example.resilience.connector.command.redis.RedisGetCommand;
import com.example.resilience.connector.command.redis.RedisSetCommand;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.model.CacheKey;
import com.example.resilience.connector.model.Result;
import com.example.resilience.connector.template.RedisTemplateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

import static com.example.resilience.connector.configuration.builder.EndpointConfigurationBuilder.anEndpointConfiguration;
import static com.example.resilience.connector.serialization.Serializers.STRING_DESERIALIZER;

public class CacheDecorator<T> implements Function<Mono<Result<T>>, Mono<Result<T>>>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheDecorator.class);

    private final CacheKey cacheKey;
    private final int cachePort;

    public CacheDecorator(CacheKey cacheKey, int cachePort)
    {
        this.cacheKey = cacheKey;
        this.cachePort = cachePort;
    }

    @Override
    public Mono<Result<T>> apply(Mono<Result<T>> originalMono)
    {
        ReactiveRedisTemplate<String, String> redisTemplate = RedisTemplateFactory.create("localhost", cachePort);

        return getFromCacheCommand(redisTemplate, cacheKey)
                .filter(Result::isSuccess)
                .switchIfEmpty(originalMono)
                .doOnNext(result -> saveToCacheAsync(redisTemplate, result));
    }

    private Mono<Result<T>> getFromCacheCommand(ReactiveRedisTemplate<String, String> redisTemplate,
            CacheKey cacheKey)
    {
        LOGGER.info("Call cache with key: " + cacheKey);

        RedisGetCommand redisGetCommand = new RedisGetCommand(redisTemplate, cacheKey);

        EndpointConfiguration configuration = anEndpointConfiguration().withTimeout(Duration.ofMillis(2000)).build();

        Mono<Result<String>> getFromCacheMono = MonoCommandBuilder.<String>aBuilder(redisGetCommand)
                .withEndpointConfiguration(configuration).withDeserializer(STRING_DESERIALIZER).build();

        return getFromCacheMono.map(Result::<T>markAsRawResponseFromCache)
                               .onErrorResume(ex -> Mono.empty());
    }

    private void saveToCacheAsync(ReactiveRedisTemplate<String, String> redisTemplate, Result<T> result)
    {
        if (result.isFromCache())
        {
            return;
        }

        RedisSetCommand redisSetStringCommand = new RedisSetCommand(redisTemplate, cacheKey.getValue(),
                result.getRawResponse());

        EndpointConfiguration configuration = anEndpointConfiguration().withTimeout(Duration.ofMillis(5000)).build();

        Mono<Result<String>> setInCacheMono = MonoCommandBuilder.<String>aBuilder(redisSetStringCommand)
                .withEndpointConfiguration(configuration).withDeserializer(STRING_DESERIALIZER).build();

        // trigger async
        setInCacheMono.subscribe();
    }
}
