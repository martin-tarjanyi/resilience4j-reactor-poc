package com.example.resilience.connector.configuration;

import java.time.Duration;

public class RateLimitConfiguration
{
    private final boolean rateLimitEnabled;
    private final Duration rateLimitPeriod;
    private final int rateLimitPermitsPerPeriod;

    public RateLimitConfiguration(boolean rateLimitEnabled, Duration rateLimitPeriod, int rateLimitPermitsPerPeriod)
    {
        this.rateLimitEnabled = rateLimitEnabled;
        this.rateLimitPeriod = rateLimitPeriod;
        this.rateLimitPermitsPerPeriod = rateLimitPermitsPerPeriod;
    }

    public boolean isRateLimitEnabled()
    {
        return rateLimitEnabled;
    }

    public Duration getRateLimitPeriod()
    {
        return rateLimitPeriod;
    }

    public int getRateLimitPermitsPerPeriod()
    {
        return rateLimitPermitsPerPeriod;
    }
}
