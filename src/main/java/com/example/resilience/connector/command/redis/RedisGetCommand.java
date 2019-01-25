package com.example.resilience.connector.command.redis;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

public class RedisGetCommand extends RedisCommand
{
    private final String cacheKey;

    public RedisGetCommand(ReactiveRedisTemplate<String, String> redisTemplate, String cacheKey)
    {
        super(redisTemplate);

        this.cacheKey = cacheKey;
    }

    @Override
    protected Mono<String> execute(ReactiveRedisTemplate<String, String> redisTemplate)
    {
        return redisTemplate.opsForValue().get(cacheKey);
    }
}
