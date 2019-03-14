package com.example.resilience.connector;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.configuration.builder.EndpointConfigurationBuilder;
import com.example.resilience.connector.model.CacheKey;
import com.example.resilience.connector.model.CommandDescriptor;
import com.example.resilience.connector.model.CommandDescriptorBuilder;
import com.example.resilience.connector.model.Result;
import com.example.resilience.connector.testcommands.SimpleTestCommand;
import org.awaitility.Duration;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class CacheDecoratorIntegrationTest extends BaseConnectorIntegrationTest
{
    private static final String CACHE_RESPONSE = "cache_response";

    @Test
    public void shouldGetFromCacheCorrectly()
    {
        ICommand command = givenCommand();
        EndpointConfiguration configuration = givenConfigurationWithEnabledCache();
        CommandDescriptor<String> commandDescriptor = givenCommandDescriptor(command, configuration);
        givenEntryInRedis(command.generateCacheKey(), CACHE_RESPONSE);

        Result<String> actualResult = whenExecuteBlocking(commandDescriptor);

        thenResponseIs(actualResult, CACHE_RESPONSE);
    }

    @Test
    public void shouldSaveToCacheCorrectly()
    {
        ICommand command = givenCommand();
        EndpointConfiguration configuration = givenConfigurationWithEnabledCache();
        CommandDescriptor<String> commandDescriptor = givenCommandDescriptor(command, configuration);

        Result<String> stringResult = whenExecuteBlocking(commandDescriptor);

        thenResponseIs(stringResult, SimpleTestCommand.RESPONSE);
        thenCacheContains(command.generateCacheKey(), SimpleTestCommand.RESPONSE);
    }

    private CommandDescriptor<String> givenCommandDescriptor(ICommand command, EndpointConfiguration configuration)
    {
        return CommandDescriptorBuilder.aCommandDescriptorWithStringResult()
                                       .withCommand(command)
                                       .withEndpointConfiguration(configuration)
                                       .build();
    }

    private SimpleTestCommand givenCommand()
    {
        return new SimpleTestCommand(3);
    }

    private EndpointConfiguration givenConfigurationWithEnabledCache()
    {
        return EndpointConfigurationBuilder.aTestEndpointConfiguration()
                                           .withCacheEnabled(true)
                                           .withCachePort(getMappedRedisPort())
                                           .build();
    }

    private void givenEntryInRedis(CacheKey key, String value)
    {
        redisTemplate.opsForValue().set(key.getValue(), value).block();
    }

    private void thenResponseIs(Result<String> stringResult, String expectedResponse)
    {
        assertThat(stringResult.getResponse()).isEqualTo(expectedResponse);
    }

    private void thenCacheContains(CacheKey expectedKey, String expectedValue)
    {
        await().atMost(Duration.FIVE_SECONDS)
               .untilAsserted(
                       () -> assertThat(redisTemplate.opsForValue().get(expectedKey.getValue()).block()).isEqualTo(
                               expectedValue));
    }
}
