package com.example.resilience.connector.command;

import reactor.core.publisher.Mono;

public interface ICommand
{
    Mono<String> execute();
}
