package com.example.resilience.connector.serialization;

public final class Serializers
{
    public static final Deserializer<String> STRING_DESERIALIZER = new StringToStringDeserializer();

    private Serializers()
    {
    }
}
