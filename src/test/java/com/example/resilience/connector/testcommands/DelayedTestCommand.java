package com.example.resilience.connector.testcommands;

import com.example.resilience.connector.command.ICommand;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class DelayedTestCommand implements ICommand<String>
{
    public static final String RESPONSE = "Sloooow....";

    private final Duration commandDuration;

    public DelayedTestCommand(Duration commandDuration)
    {
        this.commandDuration = commandDuration;
    }

    @Override
    public Mono<String> execute()
    {
        return Mono.delay(commandDuration)
                   .map(k -> RESPONSE);
    }
}
