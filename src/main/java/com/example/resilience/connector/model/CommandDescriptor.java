package com.example.resilience.connector.model;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.serialization.Deserializer;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class CommandDescriptor<T>
{
    private final EndpointConfiguration endpointConfiguration;
    private final Deserializer<T> deserializer;
    private final ICommand command;

    public CommandDescriptor(EndpointConfiguration endpointConfiguration,
            Deserializer<T> deserializer, ICommand command)
    {
        this.endpointConfiguration = endpointConfiguration;
        this.deserializer = deserializer;
        this.command = command;
    }

    public EndpointConfiguration getEndpointConfiguration()
    {
        return endpointConfiguration;
    }

    public Deserializer<T> getDeserializer()
    {
        return deserializer;
    }

    public ICommand getCommand()
    {
        return command;
    }
}
