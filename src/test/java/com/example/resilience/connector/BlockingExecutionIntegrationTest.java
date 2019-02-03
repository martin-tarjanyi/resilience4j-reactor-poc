package com.example.resilience.connector;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.model.CommandDescriptor;
import com.example.resilience.connector.model.Result;
import com.example.resilience.connector.testcommands.DelayedTestCommand;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.resilience.connector.builders.EndpointConfigurationBuilder.anEndpointConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

public class BlockingExecutionIntegrationTest extends BaseConnectorIntegrationTest
{
    private Connector connector;

    @BeforeMethod
    public void beforeMethod()
    {
        connector = new Connector(CircuitBreakerRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
                RateLimiterRegistry.ofDefaults());
    }

    @Test
    public void shouldExecuteSingleBlockingCorrectly()
    {
        // arrange
        ICommand command = givenSlowCommand(Duration.ofSeconds(1));
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().build();
        CommandDescriptor<String> descriptor = createDescriptor(endpointConfiguration, command);

        // act
        Result<String> result = connector.executeBlocking(descriptor);

        // assert
        assertThat(result).isEqualTo(Result.ofResponse(DelayedTestCommand.RESPONSE));
    }

    private ICommand givenSlowCommand(Duration duration)
    {
        return new DelayedTestCommand(duration);
    }

    @Test
    public void shouldExecuteMultipleBlockingCorrectly()
    {
        // arrange
        List<ICommand> commands = givenSlowCommands(42, Duration.ofSeconds(1));
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().withBulkhead(50).build();
        Set<CommandDescriptor<String>> descriptors = createDescriptors(commands, endpointConfiguration);

        // act
        List<Result<String>> results = connector.executeBlocking(descriptors);

        // assert
        assertThat(results).hasSize(42)
                           .extracting(Result::getResponse)
                           .allSatisfy(response -> assertThat(response).isEqualTo(DelayedTestCommand.RESPONSE));
    }

    private List<ICommand> givenSlowCommands(int n, Duration duration)
    {
        return IntStream.rangeClosed(1, n).mapToObj(i -> givenSlowCommand(duration)).collect(Collectors.toList());
    }
}
