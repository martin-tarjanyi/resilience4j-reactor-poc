package com.example.resilience.connector.testcommands;

import com.example.resilience.connector.command.BlockingCommand;
import com.example.resilience.connector.model.CacheKey;

import java.time.Duration;

public class BlockingErrorTestCommand extends BlockingCommand
{
    private final Duration duration;

    public BlockingErrorTestCommand(Duration duration)
    {
        this.duration = duration;
    }

    @Override
    protected String executeBlocking() throws InterruptedException
    {
        Thread.sleep(duration.toMillis());

        throw new TestCommandException("Blocking command failed.");
    }

    @Override
    public CacheKey generateCacheKey()
    {
        return CacheKey.valueOf(duration.toString());
    }
}
