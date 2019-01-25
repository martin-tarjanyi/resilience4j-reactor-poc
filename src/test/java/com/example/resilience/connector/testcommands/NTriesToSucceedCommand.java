package com.example.resilience.connector.testcommands;

import com.example.resilience.connector.command.ICommand;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

public class NTriesToSucceedCommand implements ICommand<String>
{
    public static final String SUCCESS_RESPONSE = "Success";
    private static final AtomicInteger atomicInteger = new AtomicInteger(1);

    private final int triesToSucceed;

    public NTriesToSucceedCommand(int triesToSucceed)
    {
        this.triesToSucceed = triesToSucceed;
    }

    @Override
    public Mono<String> execute()
    {
        return Mono.just("")
                   .map(k -> map());
    }

    private String map()
    {
        if (atomicInteger.getAndIncrement() < triesToSucceed)
        {
            throw new TestCommandException("Failure");
        }

        return SUCCESS_RESPONSE;
    }
}
