package com.example.resilience.connectior.command;

import reactor.core.publisher.Mono;

public interface ICommand<T>
{
    Mono<T> execute();
}
