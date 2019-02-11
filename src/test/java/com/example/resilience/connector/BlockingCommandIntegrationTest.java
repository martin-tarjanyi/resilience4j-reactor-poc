package com.example.resilience.connector;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.model.CommandDescriptor;
import com.example.resilience.connector.model.Result;
import com.example.resilience.connector.testcommands.BlockingErrorTestCommand;
import com.example.resilience.connector.testcommands.BlockingTestCommand;
import com.example.resilience.connector.testcommands.TestCommandException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import org.assertj.core.api.Condition;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static com.example.resilience.connector.configuration.builder.EndpointConfigurationBuilder.aTestEndpointConfiguration;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class BlockingCommandIntegrationTest extends BaseConnectorIntegrationTest
{
    @Test
    public void shouldReturnSuccessWhenBlockingCommandExecuted()
    {
        // arrange
        ICommand command = givenBlockingCommandWithSuccess(Duration.ofMillis(200));
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().build();

        // act
        Mono<String> monoResult = whenExecute(command, endpointConfiguration).map(Result::getResponse);

        // assert
        StepVerifier.create(monoResult)
                    .expectNext(BlockingTestCommand.RESPONSE)
                    .verifyComplete();
    }

    @Test
    public void shouldReturnSuccessForMultipleBlockingCommands()
    {
        // arrange
        List<ICommand> commands = givenBlockingCommandsWithSuccess(65, Duration.ofMillis(200));
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().withBulkhead(70).build();

        Set<CommandDescriptor<String>> commandDescriptors =
                createDescriptors(commands, endpointConfiguration);

        // act
        Flux<Result<String>> monoResult = connector.execute(commandDescriptors);

        // assert
        Result[] results = Collections.nCopies(65, Result.ofResponse(BlockingTestCommand.RESPONSE))
                                      .toArray(new Result[0]);

        StepVerifier.create(monoResult)
                    .expectNext(results)
                    .verifyComplete();
    }

    @Test
    public void shouldReturnAResultWithExceptionWhenBlockingCommandFails()
    {
        // arrange
        ICommand command = givenBlockingCommandWithError(Duration.ofMillis(200));
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().build();

        // act
        Mono<Result<String>> monoResult = whenExecute(command, endpointConfiguration);

        // assert
        StepVerifier.create(monoResult)
                    .assertNext(result -> assertThat(result.getThrowable()).isInstanceOf(TestCommandException.class))
                    .verifyComplete();
    }

    @Test
    public void shouldLimitConcurrencyByBulkhead()
    {
        // arrange
        List<ICommand> commands = givenBlockingCommandsWithSuccess(5, Duration.ofMillis(200));
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().withBulkhead(2).build();

        Set<CommandDescriptor<String>> commandDescriptors =
                createDescriptors(commands, endpointConfiguration);

        // act
        Flux<Result<String>> monoResult = connector.execute(commandDescriptors);

        // assert
        StepVerifier.create(monoResult)
                    .assertNext(result -> assertThat(result.getThrowable()).isInstanceOf(BulkheadFullException.class))
                    .assertNext(result -> assertThat(result.getThrowable()).isInstanceOf(BulkheadFullException.class))
                    .assertNext(result -> assertThat(result.getThrowable()).isInstanceOf(BulkheadFullException.class))
                    .expectNext(Result.ofResponse(BlockingTestCommand.RESPONSE))
                    .expectNext(Result.ofResponse(BlockingTestCommand.RESPONSE))
                    .verifyComplete();
    }

    @Test
    public void shouldLimitConcurrencyByBulkheadOnBlockingEndpoints()
    {
        // arrange
        List<ICommand> commands = givenBlockingCommandsWithSuccess(10, Duration.ofMillis(200));
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().withBulkhead(6).build();

        Set<CommandDescriptor<String>> commandDescriptors = createDescriptors(commands, endpointConfiguration);

        // act
        List<Result<String>> blockingResults = connector.executeBlocking(commandDescriptors);

        // assert
        Condition<Result<String>> errorCondition = new Condition<>(
                result -> BlockingTestCommand.RESPONSE.equals(result.getResponse()),
                "Should be result with success response.");

        Condition<Result<String>> successCondition = new Condition<>(
                result -> result.getThrowable() instanceof BulkheadFullException,
                "Should be result with bulkhead exception.");

        assertThat(blockingResults).hasSize(10)
                                   .haveExactly(6, errorCondition)
                                   .haveExactly(4, successCondition);
    }

    private List<ICommand> givenBlockingCommandsWithSuccess(int n, Duration duration)
    {
        return IntStream.rangeClosed(1, n)
                        .mapToObj(i -> givenBlockingCommandWithSuccess(duration))
                        .collect(toList());
    }

    private ICommand givenBlockingCommandWithSuccess(Duration duration)
    {
        return new BlockingTestCommand(duration);
    }

    private ICommand givenBlockingCommandWithError(Duration duration)
    {
        return new BlockingErrorTestCommand(duration);
    }

    private Mono<Result<String>> whenExecute(ICommand command, EndpointConfiguration endpointConfiguration)
    {
        CommandDescriptor<String> commandDescriptor = createDescriptor(endpointConfiguration, command);
        return connector.execute(commandDescriptor);
    }
}
