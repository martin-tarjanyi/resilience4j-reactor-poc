package com.example.resilience.connector.testcommands;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.model.CacheKey;
import reactor.core.publisher.Mono;

public class ErrorTestCommand implements ICommand
{
    @Override
    public Mono<String> execute()
    {
        return Mono.error(new TestCommandException("Exception for test purpose."));
    }

    @Override
    public CacheKey generateCacheKey()
    {
        return CacheKey.valueOf("error");
    }
}
