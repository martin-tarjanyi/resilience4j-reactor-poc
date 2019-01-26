package com.example.resilience.connector;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.model.Result;
import com.example.resilience.connector.testcommands.BlockingErrorTestCommand;
import com.example.resilience.connector.testcommands.BlockingTestCommand;
import com.example.resilience.connector.testcommands.TestCommandException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.resilience.connector.builders.EndpointConfigurationBuilder.anEndpointConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

public class BlockingCommandIntegrationTest
{
    private Connector connector;

    @BeforeMethod
    public void beforeMethod()
    {
        connector = new Connector(CircuitBreakerRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
                RateLimiterRegistry.ofDefaults());
    }

    @Test
    public void shouldReturnSuccessWhenBlockingCommandExecuted()
    {
        // arrange
        ICommand<String> command = givenBlockingCommandWithSuccess(Duration.ofMillis(200));
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().build();

        //act
        Mono<String> monoResult = whenExecute(command, endpointConfiguration).map(Result::getResponse);

        // assert
        StepVerifier.create(monoResult)
                    .expectNext(BlockingTestCommand.RESPONSE)
                    .verifyComplete();
    }

    private ICommand<String> givenBlockingCommandWithSuccess(Duration duration)
    {
        return new BlockingTestCommand(duration);
    }

    private Mono<Result<String>> whenExecute(ICommand<String> command, EndpointConfiguration endpointConfiguration)
    {
        return connector.execute(endpointConfiguration, command);
    }

    @Test
    public void shouldReturnSuccessForMultipleBlockingCommands()
    {
        // arrange
        List<ICommand<String>> commands = givenBlockingCommandsWithSuccess(65, Duration.ofMillis(200));
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().withBulkhead(70).build();

        //act
        Flux<Result<String>> monoResult = connector.execute(endpointConfiguration, commands);

        // assert
        Result[] results = Collections.nCopies(65, Result.ofSuccess(BlockingTestCommand.RESPONSE))
                                      .toArray(new Result[0]);

        StepVerifier.create(monoResult)
                    .expectNext(results)
                    .verifyComplete();
    }

    private List<ICommand<String>> givenBlockingCommandsWithSuccess(int n, Duration duration)
    {
        return IntStream.rangeClosed(1, n)
                        .mapToObj(i -> givenBlockingCommandWithSuccess(duration))
                        .collect(Collectors.toList());
    }

    @Test
    public void shouldReturnAResultWithExceptionWhenBlockingCommandFails()
    {
        // arrange
        ICommand<String> command = givenBlockingCommandWithError(Duration.ofMillis(200));
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().build();

        //act
        Mono<Result<String>> monoResult = whenExecute(command, endpointConfiguration);

        // assert
        StepVerifier.create(monoResult)
                    .assertNext(result -> assertThat(result.getThrowable()).isInstanceOf(TestCommandException.class))
                    .verifyComplete();
    }

    private ICommand<String> givenBlockingCommandWithError(Duration duration)
    {
        return new BlockingErrorTestCommand(duration);
    }
}
