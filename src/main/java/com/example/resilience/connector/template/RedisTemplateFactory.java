package com.example.resilience.connector.template;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

public class RedisTemplateFactory
{
    public static ReactiveRedisTemplate<String, String> create(String host, int port)
    {
        LettuceConnectionFactory reactiveRedisConnectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(host, port));

        reactiveRedisConnectionFactory.afterPropertiesSet();

        return new ReactiveStringRedisTemplate(reactiveRedisConnectionFactory);
    }
}
