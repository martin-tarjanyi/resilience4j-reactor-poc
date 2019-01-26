package com.example.resilience.connector.command;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public abstract class BlockingCommand implements ICommand<String>
{
    @Override
    public final Mono<String> execute()
    {
        return Mono.fromCallable(this::executeBlocking)
                   .subscribeOn(Schedulers.elastic());
    }

    protected abstract String executeBlocking() throws Exception;
}
