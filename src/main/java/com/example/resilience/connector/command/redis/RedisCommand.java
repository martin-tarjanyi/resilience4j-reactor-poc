package com.example.resilience.connector.command.redis;

import com.example.resilience.connector.command.ICommand;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

public abstract class RedisCommand implements ICommand<String>
{
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    RedisCommand(ReactiveRedisTemplate<String, String> redisTemplate)
    {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<String> execute()
    {
        return execute(redisTemplate);
    }

    protected abstract Mono<String> execute(ReactiveRedisTemplate<String, String> redisTemplate);
}
