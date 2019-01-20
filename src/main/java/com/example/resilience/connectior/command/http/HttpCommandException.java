package com.example.resilience.connectior.command.http;

public class HttpCommandException extends RuntimeException
{
    public HttpCommandException(String message)
    {
        super(message);
    }
}
