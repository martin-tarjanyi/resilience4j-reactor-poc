package com.example.resilience.connector.testcommands;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.model.CacheKey;
import lombok.ToString;
import reactor.core.publisher.Mono;

@ToString
public class SimpleTestCommand implements ICommand
{
    public static final String RESPONSE = "Simple response.";

    private final int id;

    public SimpleTestCommand(int id)
    {
        this.id = id;
    }

    @Override
    public Mono<String> execute()
    {
        return Mono.just(RESPONSE);
    }

    @Override
    public CacheKey generateCacheKey()
    {
        return CacheKey.valueOf(String.valueOf(id));
    }
}
