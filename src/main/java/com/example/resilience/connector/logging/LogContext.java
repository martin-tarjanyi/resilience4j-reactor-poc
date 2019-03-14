package com.example.resilience.connector.logging;

import com.example.resilience.connector.model.Result;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

@ToString
@EqualsAndHashCode
public final class LogContext
{
    private final Collection<Result<?>> results;

    private LogContext(Collection<Result<?>> results)
    {
        this.results = results;
    }

    public static LogContext create()
    {
        return new LogContext(new CopyOnWriteArrayList<>());
    }

    public Collection<Result<?>> getResults()
    {
        return new ArrayList<>(results);
    }

    public void add(Result<?> result)
    {
        results.add(result);
    }
}
