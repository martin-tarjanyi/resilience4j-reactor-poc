package com.example.resilience.connector.command;

import reactor.core.publisher.Mono;

public interface ICommand<T>
{
    Mono<T> execute();
}
