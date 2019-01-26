package com.example.resilience.connector;

import com.example.resilience.connector.command.redis.RedisCommand;
import com.example.resilience.connector.command.redis.RedisGetCommand;
import com.example.resilience.connector.command.redis.RedisSetCommand;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.model.Result;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.example.resilience.connector.builders.EndpointConfigurationBuilder.anEndpointConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

public class RedisIntegrationTest
{
    private GenericContainer redis;
    private Connector connector;

    @BeforeClass
    public void setUp()
    {
        redis = new GenericContainer("redis:3.0.6").withExposedPorts(6379);
        redis.start();
    }

    @AfterClass
    public void afterClass()
    {
        redis.stop();
    }

    @BeforeMethod
    public void beforeMethod()
    {
        connector = new Connector(CircuitBreakerRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
                RateLimiterRegistry.ofDefaults());
    }

    @Test
    public void shouldSetInRedisCorrectly()
    {
        // arrange
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().build();
        ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate = givenARedisTemplate("localhost");
        RedisCommand redisSetCommand = new RedisSetCommand(reactiveStringRedisTemplate, "key", "value");

        // act
        Mono<String> monoResult = connector.execute(endpointConfiguration, redisSetCommand).map(Result::getResponse);

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
        RedisCommand redisSetCommand = new RedisGetCommand(reactiveStringRedisTemplate, "key");

        // act
        Mono<String> monoResult = connector.execute(endpointConfiguration, redisSetCommand).map(Result::getResponse);

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

        // act
        Mono<Result<String>> monoResult = connector.execute(endpointConfiguration, redisSetCommand);

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
