package com.example.resilience.connector;

import com.example.resilience.connector.command.redis.RedisCommand;
import com.example.resilience.connector.command.redis.RedisGetCommand;
import com.example.resilience.connector.command.redis.RedisSetCommand;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.model.CacheKey;
import com.example.resilience.connector.model.CommandDescriptor;
import com.example.resilience.connector.model.Result;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testng.annotations.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.example.resilience.connector.builders.EndpointConfigurationBuilder.anEndpointConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

public class RedisCommandIntegrationTest extends BaseConnectorIntegrationTest
{

    @Test
    public void shouldSetInRedisCorrectly()
    {
        // arrange
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().build();
        ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate = givenARedisTemplate("localhost");
        RedisCommand redisSetCommand = new RedisSetCommand(reactiveStringRedisTemplate, "key", "value");
        CommandDescriptor<String> commandDescriptor = createDescriptor(endpointConfiguration, redisSetCommand);

        // act
        Mono<String> monoResult = whenExecuteConnectorAndExtractResponse(commandDescriptor);

        // assert
        StepVerifier.create(monoResult)
                    .expectNext("true")
                    .verifyComplete();
    }

    @Test
    public void shouldGetFromRedisCorrectly()
    {
        // arrange
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().build();
        ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate = givenARedisTemplate("localhost");
        reactiveStringRedisTemplate.opsForValue().set("key", "value").block();
        RedisCommand redisSetCommand = new RedisGetCommand(reactiveStringRedisTemplate, CacheKey.valueOf("key"));
        CommandDescriptor<String> commandDescriptor = createDescriptor(endpointConfiguration, redisSetCommand);

        // act
        Mono<String> monoResult = whenExecuteConnectorAndExtractResponse(commandDescriptor);

        // assert
        StepVerifier.create(monoResult)
                    .expectNext("value")
                    .verifyComplete();
    }

    @Test
    public void shouldReturnErrorForUnknownHost()
    {
        // arrange
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().build();
        ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate = givenARedisTemplate("unknownhost");
        RedisCommand redisSetCommand = new RedisSetCommand(reactiveStringRedisTemplate, "key", "value");
        CommandDescriptor<String> commandDescriptor = createDescriptor(endpointConfiguration, redisSetCommand);

        // act
        Mono<Result<String>> monoResult = whenExecuteConnector(commandDescriptor);

        // assert
        StepVerifier.create(monoResult)
                    .assertNext(result -> assertThat(result.getThrowable()).isInstanceOf(
                            RedisConnectionFailureException.class))
                    .verifyComplete();
    }

    private ReactiveRedisTemplate<String, String> givenARedisTemplate(String host)
    {
        LettuceConnectionFactory reactiveRedisConnectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(host, redis.getMappedPort(6379)));

        reactiveRedisConnectionFactory.afterPropertiesSet();

        return new ReactiveStringRedisTemplate(reactiveRedisConnectionFactory);
    }
}
