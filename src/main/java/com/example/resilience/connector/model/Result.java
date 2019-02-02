package com.example.resilience.connector.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Result<T>
{
    private final T response;
    private final String rawResponse;
    private final Throwable throwable;
    private final boolean fromCache;

    private Result(T response, String rawResponse, Throwable throwable, boolean fromCache)
    {
        this.response = response;
        this.rawResponse = rawResponse;
        this.throwable = throwable;
        this.fromCache = fromCache;
    }

    public static <T> Result<T> ofError(Throwable throwable)
    {
        return new Result<>(null, null, throwable, false);
    }

    public static <T> Result<T> ofResponse(T response)
    {
        return new Result<>(response, null, null, false);
    }

    public static Result<String> ofRawResponse(String rawResponse)
    {
        return new Result<>(rawResponse, null, null, false);
    }

    public T getResponse()
    {
        return response;
    }

    public String getRawResponse()
    {
        return rawResponse;
    }

    public Throwable getThrowable()
    {
        return throwable;
    }

    public boolean isFromCache()
    {
        return fromCache;
    }

    public Result<T> markAsFromCache()
    {
        return new Result<>(this.response, this.rawResponse, this.throwable, true);
    }

    public <U> Result<U> addDeserializedResponse(U response)
    {
        return new Result<>(response, null, this.throwable, this.fromCache);
    }

    public boolean isSuccess()
    {
        return throwable == null;
    }
}
