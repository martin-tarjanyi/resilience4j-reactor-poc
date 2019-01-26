package com.example.resilience.connector;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.model.Result;
import com.example.resilience.connector.testcommands.NTriesToSucceedCommand;
import com.example.resilience.connector.testcommands.TestCommandException;
import com.example.resilience.connector.testcommands.TestDelayedCommand;
import com.example.resilience.connector.testcommands.TestErrorCommand;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static com.example.resilience.connector.builders.EndpointConfigurationBuilder.anEndpointConfiguration;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class ResilienceIntegrationTest
{
    private Connector connector;

    @BeforeMethod
    public void beforeMethod()
    {
        connector = new Connector(CircuitBreakerRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
                RateLimiterRegistry.ofDefaults());
    }

    @Test
    public void shouldExecuteSuccessfully()
    {
        // arrange
        ICommand<String> httpCommand = givenSlowCommand(Duration.ofSeconds(1));
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().build();

        //act
        Mono<String> result = whenExecute(httpCommand, endpointConfiguration).map(Result::getResponse);

        // assert
        StepVerifier.create(result)
                    .expectNext(TestDelayedCommand.RESPONSE)
                    .verifyComplete();
    }

    @Test
    public void shouldTimeout()
    {
        // arrange
        ICommand<String> httpCommand = givenSlowCommand(Duration.ofSeconds(1));
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration()
                .withTimeout(Duration.ofMillis(500))
                .build();

        // act
        Mono<Result<String>> monoResult = whenExecute(httpCommand, endpointConfiguration);

        // assert
        StepVerifier.create(monoResult)
                    .assertNext(result -> assertException(result, TimeoutException.class))
                    .verifyComplete();
    }

    @Test
    public void shouldActivateBulkhead()
    {
        // arrange
        List<ICommand<String>> commands = givenSlowCommands(3);
        EndpointConfiguration endpointConfiguration = givenConfigurationWithBulkHead(2);

        // act
        Flux<Result<String>> results = connector.execute(endpointConfiguration, commands);

        // assert
        StepVerifier.create(results)
                    .assertNext(result -> assertException(result, BulkheadFullException.class))
                    .expectNext(Result.ofSuccess(TestDelayedCommand.RESPONSE))
                    .expectNext(Result.ofSuccess(TestDelayedCommand.RESPONSE))
                    .verifyComplete();
    }

    @Test
    public void shouldActivateCircuitBreaker()
    {
        // arrange
        List<ICommand<String>> commands = givenErrorCommands(5);
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().withCircuitBreakerBufferSize(3).build();

        // act
        // sequential execution with concatmap to have stable result
        Flux<Result<String>> results = Flux.fromIterable(commands)
                                           .concatMap(command -> whenExecute(command, endpointConfiguration));

        // assert
        StepVerifier.create(results)
                    .assertNext(result -> assertException(result, TestCommandException.class))
                    .assertNext(result -> assertException(result, TestCommandException.class))
                    .assertNext(result -> assertException(result, TestCommandException.class))
                    .assertNext(result -> assertException(result, CircuitBreakerOpenException.class))
                    .assertNext(result -> assertException(result, CircuitBreakerOpenException.class))
                    .verifyComplete();
    }

    @Test
    public void shouldSucceedWithRetry()
    {
        ICommand<String> command = new NTriesToSucceedCommand(3);
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().withRetries(2).build();

        // act
        Mono<String> resultMono = whenExecute(command, endpointConfiguration).map(Result::getResponse);

        // assert
        StepVerifier.create(resultMono)
                    .expectNext(NTriesToSucceedCommand.SUCCESS_RESPONSE)
                    .verifyComplete();
    }

    @Test
    public void shouldFailWithNotEnoughRetry()
    {
        ICommand<String> command = new NTriesToSucceedCommand(3);
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().withRetries(1).build();

        // act
        Mono<Result<String>> resultMono = whenExecute(command, endpointConfiguration);

        // assert
        StepVerifier.create(resultMono)
                    .assertNext(result -> assertException(result, TestCommandException.class))
                    .verifyComplete();
    }

    private EndpointConfiguration givenConfigurationWithBulkHead(int bulkhead)
    {
        return anEndpointConfiguration().withBulkhead(bulkhead).build();
    }

    private List<ICommand<String>> givenSlowCommands(int numberOfCommands)
    {
        return IntStream.rangeClosed(1, numberOfCommands)
                        .mapToObj(i -> givenSlowCommand(Duration.ofSeconds(1)))
                        .collect(toList());
    }

    private List<ICommand<String>> givenErrorCommands(int numberOfCommands)
    {
        return IntStream.rangeClosed(1, numberOfCommands)
                        .mapToObj(i -> givenErrorCommand())
                        .collect(toList());
    }

    private ICommand<String> givenSlowCommand(Duration commandDuration)
    {
        return new TestDelayedCommand(commandDuration);
    }

    private ICommand<String> givenErrorCommand()
    {
        return new TestErrorCommand();
    }

    private Mono<Result<String>> whenExecute(ICommand<String> command, EndpointConfiguration endpointConfiguration)
    {
        return connector.execute(endpointConfiguration, command);
    }

    private void assertException(Result<String> result, Class<? extends Throwable> type)
    {
        assertThat(result.getThrowable()).isInstanceOf(type);
    }
}
