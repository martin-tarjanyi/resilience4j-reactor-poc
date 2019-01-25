package com.example.resilience.connector.command.http;

public class HttpCommandException extends RuntimeException
{
    HttpCommandException(String message)
    {
        super(message);
    }
}
