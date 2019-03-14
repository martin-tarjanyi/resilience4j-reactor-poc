package com.example.resilience.connector.command.http;

public class HttpCommandException extends RuntimeException
{
    public HttpCommandException(String message)
    {
        super(message);
    }
}
