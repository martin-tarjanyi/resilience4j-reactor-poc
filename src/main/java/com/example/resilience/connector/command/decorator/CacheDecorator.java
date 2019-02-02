package com.example.resilience.connector.command.decorator;

import com.example.resilience.connector.command.MonoCommandBuilder;
import com.example.resilience.connector.command.redis.RedisGetCommand;
import com.example.resilience.connector.command.redis.RedisSetCommand;
import com.example.resilience.connector.model.CacheKey;
import com.example.resilience.connector.model.Result;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class CacheDecorator
{
    private final Mono<Result<String>> originalMono;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final CacheKey cacheKey;

    public CacheDecorator(Mono<Result<String>> originalMono, ReactiveRedisTemplate<String, String> redisTemplate,
            CacheKey cacheKey)
    {
        this.originalMono = originalMono;
        this.redisTemplate = redisTemplate;
        this.cacheKey = cacheKey;
    }

    public Mono<Result<String>> decorate()
    {
        return getFromCacheCommand(cacheKey)
                .filter(Result::isSuccess)
                .switchIfEmpty(originalMono)
                .doOnNext(this::saveToCacheAsync);
    }

    private Mono<Result<String>> getFromCacheCommand(CacheKey cacheKey)
    {
        RedisGetCommand redisGetStringCommand = new RedisGetCommand(redisTemplate, cacheKey);

        Mono<Result<String>> getFromCacheMono = MonoCommandBuilder.aBuilder(redisGetStringCommand)
                                                                  .withTimeout(Duration.ofMillis(200))
                                                                  .build();

        return getFromCacheMono.map(Result::markAsFromCache).onErrorResume(ex -> Mono.empty());
    }

    private void saveToCacheAsync(Result<String> result)
    {
        if (result.isFromCache())
        {
            return;
        }

        RedisSetCommand redisSetStringCommand = new RedisSetCommand(redisTemplate, cacheKey.getValue(),
                result.getRawResponse());

        Mono<Result<String>> setInCacheMono = MonoCommandBuilder.aBuilder(redisSetStringCommand)
                                                                .withTimeout(Duration.ofMillis(500))
                                                                .build();
        setInCacheMono.subscribe();
    }
}
