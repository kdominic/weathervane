package com.assignment.spring.webclient;

import com.assignment.spring.config.WeatherServerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Log4j2
@RequiredArgsConstructor
public class WeatherApiWebClient {

    private final WeatherServerConfig weatherServerConfig;

    @Bean("weatherServerClient")
    public WebClient weatherServerClient() {
        return WebClient.builder()
                .baseUrl(weatherServerConfig.getUrlBase())
                .filter(ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
                    log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
                    log.debug("--- http headers: ---");
                    clientRequest
                            .headers()
                            .forEach((name, values) -> log.debug("{}: {}", name, values));
                    return Mono.just(clientRequest);
                }))
                .filter(ExchangeFilterFunction.ofResponseProcessor(response -> {
                    HttpStatus status = response.statusCode();
                    log.debug("Returned status code: {} ({})", status.value(), status.getReasonPhrase());
                    return Mono.just(response);
                }))
                .build();
    }
}
