package com.example.resilience.connector.model;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.serialization.Deserializer;
import com.example.resilience.connector.serialization.StringToStringDeserializer;

public final class CommandDescriptorBuilder<T>
{
    private EndpointConfiguration endpointConfiguration;
    private Deserializer<T> deserializer;
    private ICommand command;

    private CommandDescriptorBuilder()
    {
    }

    public static CommandDescriptorBuilder<String> aCommandDescriptorWithStringResult()
    {
        return CommandDescriptorBuilder.<String>aCommandDescriptor().withDeserializer(new StringToStringDeserializer());
    }

    public CommandDescriptorBuilder<T> withDeserializer(Deserializer<T> deserializer)
    {
        this.deserializer = deserializer;
        return this;
    }

    public static <T> CommandDescriptorBuilder<T> aCommandDescriptor()
    {
        return new CommandDescriptorBuilder<>();
    }

    public CommandDescriptorBuilder<T> withEndpointConfiguration(EndpointConfiguration endpointConfiguration)
    {
        this.endpointConfiguration = endpointConfiguration;
        return this;
    }

    public CommandDescriptorBuilder<T> withCommand(ICommand command)
    {
        this.command = command;
        return this;
    }

    public CommandDescriptor<T> build()
    {
        return new CommandDescriptor<>(endpointConfiguration, deserializer, command);
    }
}
