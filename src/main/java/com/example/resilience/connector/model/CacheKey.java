package com.example.resilience.connector.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class CacheKey
{
    private final String value;

    private CacheKey(String value)
    {
        this.value = value;
    }

    public static CacheKey valueOf(String key)
    {
        return new CacheKey(key);
    }

    public String getValue()
    {
        return value;
    }
}
