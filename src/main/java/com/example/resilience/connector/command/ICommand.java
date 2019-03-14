package com.example.resilience.connector.command;

import com.example.resilience.connector.model.CacheKey;
import reactor.core.publisher.Mono;

public interface ICommand
{
    Mono<String> execute();

    CacheKey generateCacheKey();
}
