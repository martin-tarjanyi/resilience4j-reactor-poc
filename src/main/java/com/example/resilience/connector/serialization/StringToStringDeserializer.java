package com.example.resilience.connector.serialization;

public class StringToStringDeserializer implements Deserializer<String>
{
    @Override
    public String deserialize(String raw)
    {
        return raw;
    }
}
