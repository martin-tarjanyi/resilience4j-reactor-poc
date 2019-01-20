package com.example.resilience.connectior.command.http;

import com.example.resilience.connectior.command.ICommand;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class HttpCommand implements ICommand<String>
{
    private final WebClient webClient;
    private final String uri;

    public HttpCommand(WebClient webClient, String uri)
    {
        this.webClient = webClient;
        this.uri = uri;
    }

    @Override
    public Mono<String> execute()
    {
        return webClient.get()
                        .uri(uri)
                        .exchange()
                        .flatMap(httpResponse -> verify(httpResponse))
                        .flatMap(o -> o.bodyToMono(String.class));
    }

    private Mono<ClientResponse> verify(ClientResponse httpResponse)
    {
        if (httpResponse.statusCode().isError())
        {
            return Mono.error(new HttpCommandException("Http command failed with status code: " + httpResponse.statusCode()));
        }

        return Mono.just(httpResponse);
    }
}
