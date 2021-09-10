package com.assignment.spring.service;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface WeatherService {
    Mono<ResponseEntity<Object>> retrieveAndSaveWeather(String city);
}
