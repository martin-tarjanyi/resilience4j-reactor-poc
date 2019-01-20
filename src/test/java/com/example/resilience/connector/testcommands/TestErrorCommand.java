package com.example.resilience.connector.testcommands;

import com.example.resilience.connectior.command.ICommand;
import reactor.core.publisher.Mono;

public class TestErrorCommand implements ICommand<String>
{
    @Override
    public Mono<String> execute()
    {
        return Mono.error(new TestCommandException("Exception for test purpose."));
    }
}
