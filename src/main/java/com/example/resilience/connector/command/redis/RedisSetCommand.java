package com.example.resilience.connector.command.redis;

import com.example.resilience.connector.model.CacheKey;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

public class RedisSetCommand extends RedisCommand
{
    private final String cacheKey;
    private final String cacheValue;

    public RedisSetCommand(ReactiveRedisTemplate<String, String> redisTemplate, String cacheKey, String cacheValue)
    {
        super(redisTemplate);

        this.cacheKey = cacheKey;
        this.cacheValue = cacheValue;
    }

    @Override
    protected Mono<String> execute(ReactiveRedisTemplate<String, String> redisTemplate)
    {
        return redisTemplate.opsForValue().set(cacheKey, cacheValue).map(Object::toString);
    }

    @Override
    public CacheKey cacheKey()
    {
        throw new IllegalStateException("Cache command can not be cached.");
    }
}
