package com.example.resilience.connectior;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Result<T>
{
    private final T response;
    private final Throwable throwable;

    private Result(T response, Throwable throwable)
    {
        this.response = response;
        this.throwable = throwable;
    }

    public static <T> Result<T> ofError(Throwable throwable)
    {
        return new Result<>(null, throwable);
    }

    public static <T> Result<T> ofSuccess(T response)
    {
        return new Result<>(response, null);
    }

    public T getResponse()
    {
        return response;
    }

    public Throwable getThrowable()
    {
        return throwable;
    }
}
