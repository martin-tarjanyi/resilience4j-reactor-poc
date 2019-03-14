package com.example.resilience.connector.command.redis;

import com.example.resilience.connector.model.CacheKey;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

public class RedisGetCommand extends RedisCommand
{
    private final CacheKey cacheKey;

    public RedisGetCommand(ReactiveRedisTemplate<String, String> redisTemplate, CacheKey cacheKey)
    {
        super(redisTemplate);

        this.cacheKey = cacheKey;
    }

    @Override
    protected Mono<String> execute(ReactiveRedisTemplate<String, String> redisTemplate)
    {
        return redisTemplate.opsForValue().get(cacheKey.getValue());
    }

    @Override
    public CacheKey generateCacheKey()
    {
        throw new IllegalStateException("Cache command can not be cached.");
    }
}
