package com.example.resilience.connector;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.configuration.ConnectorConfiguration;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.model.CommandDescriptor;
import com.example.resilience.connector.model.CommandDescriptorBuilder;
import com.example.resilience.connector.model.Result;
import com.example.resilience.connector.template.RedisTemplateFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.containers.GenericContainer;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@ContextConfiguration(classes = ConnectorConfiguration.class)
public class BaseConnectorIntegrationTest extends AbstractTestNGSpringContextTests
{
    private static final int REDIS_CONTAINER_PORT = 6379;

    protected static GenericContainer redis;
    protected static ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    protected Connector connector;

    @BeforeSuite
    public void beforeSuite()
    {
        redis = new GenericContainer("redis:3.0.6").withExposedPorts(REDIS_CONTAINER_PORT);
        redis.start();

        redisTemplate = RedisTemplateFactory.create("localhost", getMappedRedisPort());
    }

    @AfterSuite
    public void afterSuite()
    {
        redis.stop();
    }

    @BeforeMethod
    public void beforeMethod()
    {
        redisTemplate.keys("*")
                     .flatMap(key -> redisTemplate.delete(key))
                     .collectList()
                     .block();
    }

    protected int getMappedRedisPort()
    {
        return redis.getMappedPort(REDIS_CONTAINER_PORT);
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

    protected Result<String> whenExecuteBlocking(CommandDescriptor<String> commandDescriptor)
    {
        return connector.executeBlocking(commandDescriptor);
    }

    protected Mono<String> whenExecuteConnectorAndExtractResponse(CommandDescriptor<String> commandDescriptor)
    {
        return connector.execute(commandDescriptor).map(Result::getResponse);
    }
}
