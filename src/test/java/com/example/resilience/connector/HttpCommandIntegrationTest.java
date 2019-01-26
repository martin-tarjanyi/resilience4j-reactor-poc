package com.example.resilience.connector;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.command.http.HttpCommand;
import com.example.resilience.connector.command.http.HttpCommandException;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.model.Result;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.web.reactive.function.client.WebClient;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.example.resilience.connector.builders.EndpointConfigurationBuilder.anEndpointConfiguration;
import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpCommandIntegrationTest
{
    private static final String HTTP_RESPONSE_BODY = "HTTP response body";
    private static final WebClient WEB_CLIENT = WebClient.create();

    private Connector connector;
    private WireMockServer wireMockServer;

    @BeforeClass
    public void setUp()
    {
        wireMockServer = new WireMockServer(6060);

        wireMockServer.stubFor(get("/ok").willReturn(responseDefinition().withBody(HTTP_RESPONSE_BODY)));
        wireMockServer.stubFor(get("/error").willReturn(responseDefinition().withStatus(500)));

        wireMockServer.start();
    }

    @AfterClass
    public void afterClass()
    {
        wireMockServer.stop();
    }

    @BeforeMethod
    public void beforeMethod()
    {
        connector = new Connector(CircuitBreakerRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
                RateLimiterRegistry.ofDefaults());
    }

    @Test
    public void shouldExecuteSuccessfullyWhenHttpServerRespondsSuccessfully()
    {
        // arrange
        ICommand<String> httpCommand = givenHttpCommand("http://localhost:6060/ok");
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().build();

        //act
        Mono<String> result = whenExecute(httpCommand, endpointConfiguration).map(Result::getResponse);

        // assert
        StepVerifier.create(result)
                    .expectNext(HTTP_RESPONSE_BODY)
                    .verifyComplete();
    }

    @Test
    public void shouldReturnAResultWithExceptionWhenHttpServerRespondsWith500()
    {
        // arrange
        ICommand<String> httpCommand = givenHttpCommand("http://localhost:6060/error");
        EndpointConfiguration endpointConfiguration = anEndpointConfiguration().build();

        //act
        Mono<Result<String>> monoResult = whenExecute(httpCommand, endpointConfiguration);

        // assert
        StepVerifier.create(monoResult)
                    .assertNext(result -> assertThat(result.getThrowable()).isInstanceOf(HttpCommandException.class))
                    .verifyComplete();
    }

    private ICommand<String> givenHttpCommand(String uri)
    {
        return new HttpCommand(WEB_CLIENT, uri);
    }

    private Mono<Result<String>> whenExecute(ICommand<String> command, EndpointConfiguration endpointConfiguration)
    {
        return connector.execute(endpointConfiguration, command);
    }
}
