package com.example.resilience.connector.testcommands;

import com.example.resilience.connector.command.BlockingCommand;
import com.example.resilience.connector.command.MonoCommandBuilder;
import com.example.resilience.connector.model.CacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class BlockingTestCommand extends BlockingCommand
{
    public static final String RESPONSE = "BLOCKING_COMMAND_RESPONSE";
    private static final Logger LOGGER = LoggerFactory.getLogger(MonoCommandBuilder.class);

    private final Duration duration;

    public BlockingTestCommand(Duration duration)
    {
        this.duration = duration;
    }

    @Override
    protected String executeBlocking() throws InterruptedException
    {
        Thread.sleep(duration.toMillis());

        LOGGER.info("Inside blocking");
        return RESPONSE;
    }

    @Override
    public CacheKey cacheKey()
    {
        return CacheKey.valueOf(duration.toString());
    }
}
