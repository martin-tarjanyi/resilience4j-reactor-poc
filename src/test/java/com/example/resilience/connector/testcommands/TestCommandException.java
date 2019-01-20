package com.example.resilience.connector.testcommands;

public class TestCommandException extends RuntimeException
{
    public TestCommandException(String message)
    {
        super(message);
    }
}
