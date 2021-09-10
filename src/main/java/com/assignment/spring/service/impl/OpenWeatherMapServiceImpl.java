package com.assignment.spring.service.impl;

import com.assignment.spring.config.WeatherServerConfig;
import com.assignment.spring.exception.CityNotFoundException;
import com.assignment.spring.exception.GenericApiException;
import com.assignment.spring.service.OpenWeatherMapService;
import com.assignment.spring.webclient.model.WeatherApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class OpenWeatherMapServiceImpl implements OpenWeatherMapService {

    @Qualifier("weatherServerClient")
    private final WebClient weatherServerClient;
    private final WeatherServerConfig weatherServerConfig;

    @Override
    public Mono<WeatherApiResponse> fetchWeather(String city) {
        log.info("Called fetchWeather for: " + city);

        return weatherServerClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(weatherServerConfig.getUrlEp())
                        .queryParam("q", city)
                        .queryParam("APPID", weatherServerConfig.getAppId())
                        .build())
                .exchange()
                .timeout(Duration.ofMillis(weatherServerConfig.getTimeout()))
                .retryWhen(Retry.backoff(weatherServerConfig.getRetries(), Duration.ofSeconds(1)).jitter(0.75))
                .flatMap(clientResponse -> {

                    switch (clientResponse.statusCode()) {
                        case OK:
                            return clientResponse.bodyToMono(WeatherApiResponse.class);
                        case UNAUTHORIZED: {
                            log.error("Wrong API Key for weather server source");
                            return Mono.error(new GenericApiException("4011", "Weather API authorization failed"));
                        }
                        case NOT_FOUND: {
                            return clientResponse
                                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                                    })
                                    .flatMap(resp -> {
                                        Object objKeyVal = resp.get("message");
                                        if (objKeyVal == null) {
                                            log.error("Unknown error while calling weather API");
                                            return Mono.error(new GenericApiException("5005", "Error calling weather API"));
                                        } else {
                                            if ("city not found".equals(String.valueOf(objKeyVal))) {
                                                log.error("City not found");
                                                return Mono.error(new CityNotFoundException(city));
                                            } else {
                                                log.error("Wrong weather API endpoint");
                                                return Mono.error(new GenericApiException("5004", "Weather API not found"));
                                            }
                                        }
                                    });
                        }
                    }

                    log.error(new StringBuilder("Error fetching weather data: ")
                            .append(clientResponse.statusCode().value())
                            .append(" - ").append(clientResponse.statusCode().getReasonPhrase()).toString());

                    return Mono.error(new GenericApiException("5003", "Cannot fetch weather data"));
                });

    }
}
