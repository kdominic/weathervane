package com.assignment.spring.service;

import com.assignment.spring.webclient.model.WeatherApiResponse;
import reactor.core.publisher.Mono;

public interface OpenWeatherMapService {
    Mono<WeatherApiResponse> fetchWeather(String city);
}
