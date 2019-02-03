package com.example.resilience.connector;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.model.CommandDescriptor;
import com.example.resilience.connector.model.CommandDescriptorBuilder;
import com.example.resilience.connector.model.Result;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class BaseConnectorIntegrationTest
{
    protected static GenericContainer redis;

    protected Connector connector;

    @BeforeSuite
    public void beforeSuite()
    {
        redis = new GenericContainer("redis:3.0.6").withExposedPorts(6379);
        redis.start();
    }

    @AfterSuite
    public void afterSuite()
    {
        redis.stop();
    }

    @BeforeMethod
    public void beforeMethod()
    {
        connector = new Connector(CircuitBreakerRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
                RateLimiterRegistry.ofDefaults());
    }

    protected Set<CommandDescriptor<String>> createDescriptors(List<ICommand> commands,
            EndpointConfiguration endpointConfiguration)
    {
        return commands.stream().map(command -> createDescriptor(endpointConfiguration, command)).collect(toSet());
    }

    protected CommandDescriptor<String> createDescriptor(EndpointConfiguration endpointConfiguration, ICommand command)
    {
        return CommandDescriptorBuilder.aCommandDescriptorWithStringResult()
                                       .withCommand(command)
                                       .withEndpointConfiguration(
                                               endpointConfiguration)
                                       .build();
    }

    protected Mono<Result<String>> whenExecuteConnector(CommandDescriptor<String> commandDescriptor)
    {
        return connector.execute(commandDescriptor);
    }

    protected Mono<String> whenExecuteConnectorAndExtractResponse(CommandDescriptor<String> commandDescriptor)
    {
        return connector.execute(commandDescriptor).map(Result::getResponse);
    }
}
