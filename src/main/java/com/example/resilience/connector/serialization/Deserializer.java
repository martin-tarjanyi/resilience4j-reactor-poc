package com.example.resilience.connector.serialization;

public interface Deserializer<T>
{
    T deserialize(String raw);
}
