package com.example.resilience.connector;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.model.CommandDescriptor;
import com.example.resilience.connector.model.Result;
import com.example.resilience.connector.testcommands.DelayedTestCommand;
import com.example.resilience.connector.testcommands.ErrorTestCommand;
import com.example.resilience.connector.testcommands.NTriesToSucceedTestCommand;
import com.example.resilience.connector.testcommands.TestCommandException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static com.example.resilience.connector.configuration.builder.EndpointConfigurationBuilder.aTestEndpointConfiguration;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;

public class ResilienceIntegrationTest extends BaseConnectorIntegrationTest
{
    @Test
    public void shouldExecuteSuccessfully()
    {
        // arrange
        ICommand command = givenSlowCommand(Duration.ofSeconds(1));
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().build();
        CommandDescriptor<String> descriptor = createDescriptor(endpointConfiguration, command);

        //act
        Mono<String> result = whenExecuteConnectorAndExtractResponse(descriptor);

        // assert
        StepVerifier.create(result)
                    .expectNext(DelayedTestCommand.RESPONSE)
                    .verifyComplete();
    }

    @Test
    public void shouldExecuteMultipleCommandsSuccessfully()
    {
        // arrange
        List<ICommand> command = givenSlowCommands(130);
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().withBulkhead(130).build();
        Set<CommandDescriptor<String>> descriptors = createDescriptors(command, endpointConfiguration);

        //act
        Flux<String> result = connector.execute(descriptors).map(Result::getResponse);

        String[] expectedResponses = Collections.nCopies(130, DelayedTestCommand.RESPONSE).toArray(String[]::new);

        // assert
        StepVerifier.create(result)
                    .expectNext(expectedResponses)
                    .expectComplete()
                    .verify(Duration.ofMillis(2000));
    }

    @Test
    public void shouldTimeout()
    {
        // arrange
        ICommand command = givenSlowCommand(Duration.ofSeconds(1));
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration()
                .withTimeout(Duration.ofMillis(500))
                .build();
        CommandDescriptor<String> descriptor = createDescriptor(endpointConfiguration, command);

        //act
        Mono<Result<String>> monoResult = whenExecuteConnector(descriptor);

        // assert
        StepVerifier.create(monoResult)
                    .assertNext(result -> assertException(result, TimeoutException.class))
                    .verifyComplete();
    }

    @Test
    public void shouldActivateCircuitBreaker()
    {
        // arrange
        List<ICommand> commands = givenErrorCommands(5);
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().withCircuitBreakerBufferSize(3)
                                                                                  .build();

        // act
        // sequential execution with concatmap to have stable result
        Flux<Result<String>> results = Flux.fromIterable(commands)
                                           .map(command -> createDescriptor(endpointConfiguration, command))
                                           .concatMap(this::whenExecuteConnector);

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
    public void shouldActivateCircuitBreakerWithTimeout()
    {
        // arrange
        List<ICommand> commands = givenSlowCommands(5);
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().withTimeout(Duration.ofMillis(100))
                                                                                  .withCircuitBreakerBufferSize(3)
                                                                                  .build();

        // act
        // sequential execution with concatmap to have stable result
        Flux<Result<String>> results = Flux.fromIterable(commands)
                                           .map(command -> createDescriptor(endpointConfiguration, command))
                                           .concatMap(this::whenExecuteConnector);

        // assert
        StepVerifier.create(results)
                    .assertNext(result -> assertException(result, TimeoutException.class))
                    .assertNext(result -> assertException(result, TimeoutException.class))
                    .assertNext(result -> assertException(result, TimeoutException.class))
                    .assertNext(result -> assertException(result, CircuitBreakerOpenException.class))
                    .assertNext(result -> assertException(result, CircuitBreakerOpenException.class))
                    .verifyComplete();
    }

    @Test
    public void shouldActivateBulkhead()
    {
        // arrange
        List<ICommand> commands = givenSlowCommands(3);
        EndpointConfiguration endpointConfiguration = givenConfigurationWithBulkHead(2);
        Set<CommandDescriptor<String>> descriptors = createDescriptors(commands, endpointConfiguration);

        // act
        Flux<Result<String>> results = connector.execute(descriptors);

        // assert
        StepVerifier.create(results)
                    .assertNext(result -> assertException(result, BulkheadFullException.class))
                    .expectNext(Result.ofResponse(DelayedTestCommand.RESPONSE))
                    .expectNext(Result.ofResponse(DelayedTestCommand.RESPONSE))
                    .verifyComplete();
    }

    @Test
    public void shouldSucceedWithRetry()
    {
        ICommand command = new NTriesToSucceedTestCommand(3);
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().withRetries(2).build();
        CommandDescriptor<String> descriptor = createDescriptor(endpointConfiguration, command);

        //act
        Mono<String> resultMono = whenExecuteConnectorAndExtractResponse(descriptor);

        // assert
        StepVerifier.create(resultMono)
                    .expectNext(NTriesToSucceedTestCommand.SUCCESS_RESPONSE)
                    .verifyComplete();
    }

    @Test
    public void shouldFailWithNotEnoughRetry()
    {
        ICommand command = new NTriesToSucceedTestCommand(3);
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().withRetries(1).build();
        CommandDescriptor<String> descriptor = createDescriptor(endpointConfiguration, command);

        //act
        Mono<Result<String>> resultMono = whenExecuteConnector(descriptor);

        // assert
        StepVerifier.create(resultMono)
                    .assertNext(result -> assertException(result, TestCommandException.class))
                    .verifyComplete();
    }

    private ICommand givenSlowCommand(Duration commandDuration)
    {
        return new DelayedTestCommand(commandDuration);
    }

    private List<ICommand> givenErrorCommands(int numberOfCommands)
    {
        return IntStream.rangeClosed(1, numberOfCommands)
                        .mapToObj(i -> givenErrorCommand())
                        .collect(toUnmodifiableList());
    }

    private EndpointConfiguration givenConfigurationWithBulkHead(int bulkhead)
    {
        return aTestEndpointConfiguration().withBulkhead(bulkhead).build();
    }

    private ICommand givenErrorCommand()
    {
        return new ErrorTestCommand();
    }

    private List<ICommand> givenSlowCommands(int numberOfCommands)
    {
        return IntStream.rangeClosed(1, numberOfCommands)
                        .mapToObj(i -> givenSlowCommand(Duration.ofSeconds(1)))
                        .collect(toList());
    }

    private void assertException(Result<String> result, Class<? extends Throwable> type)
    {
        assertThat(result.getThrowable()).isInstanceOf(type);
    }
}
