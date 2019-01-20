package com.example.resilience.connector.testcommands;

import com.example.resilience.connectior.command.ICommand;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class TestDelayedCommand implements ICommand<String>
{
    public static final String SLOW_RESPONSE = "Sloooow....";

    private final Duration commandDuration;

    public TestDelayedCommand(Duration commandDuration)
    {
        this.commandDuration = commandDuration;
    }

    @Override
    public Mono<String> execute()
    {
        return Mono.delay(commandDuration)
                   .map(k -> SLOW_RESPONSE);
    }
}
