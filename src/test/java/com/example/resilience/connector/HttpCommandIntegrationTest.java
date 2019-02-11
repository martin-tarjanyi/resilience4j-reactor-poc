package com.example.resilience.connector;

import com.example.resilience.connector.command.ICommand;
import com.example.resilience.connector.command.http.HttpCommand;
import com.example.resilience.connector.command.http.HttpCommandException;
import com.example.resilience.connector.configuration.EndpointConfiguration;
import com.example.resilience.connector.model.CommandDescriptor;
import com.example.resilience.connector.model.CommandDescriptorBuilder;
import com.example.resilience.connector.model.Result;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.web.reactive.function.client.WebClient;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.example.resilience.connector.configuration.builder.EndpointConfigurationBuilder.aTestEndpointConfiguration;
import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpCommandIntegrationTest extends BaseConnectorIntegrationTest
{
    private static final String HTTP_RESPONSE_BODY = "HTTP response body";
    private static final WebClient WEB_CLIENT = WebClient.create();

    private WireMockServer wireMockServer;

    @BeforeClass
    public void beforeClass()
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

    @Test
    public void shouldExecuteSuccessfullyWhenHttpServerRespondsSuccessfully()
    {
        // arrange
        ICommand httpCommand = givenHttpCommand("http://localhost:6060/ok");
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().build();

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
        ICommand httpCommand = givenHttpCommand("http://localhost:6060/error");
        EndpointConfiguration endpointConfiguration = aTestEndpointConfiguration().build();

        //act
        Mono<Result<String>> monoResult = whenExecute(httpCommand, endpointConfiguration);

        // assert
        StepVerifier.create(monoResult)
                    .assertNext(result -> assertThat(result.getThrowable()).isInstanceOf(HttpCommandException.class))
                    .verifyComplete();
    }

    private ICommand givenHttpCommand(String uri)
    {
        return new HttpCommand(WEB_CLIENT, uri);
    }

    private Mono<Result<String>> whenExecute(ICommand command, EndpointConfiguration endpointConfiguration)
    {
        CommandDescriptor<String> commandDescriptor = CommandDescriptorBuilder.aCommandDescriptorWithStringResult()
                                                                              .withCommand(command)
                                                                              .withEndpointConfiguration(
                                                                                      endpointConfiguration)
                                                                              .build();

        return connector.execute(commandDescriptor);
    }
}
