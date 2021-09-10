package com.assignment.spring.service.impl;

import com.assignment.spring.dto.ErrorResponse;
import com.assignment.spring.dto.WeatherResponse;
import com.assignment.spring.exception.CityNotFoundException;
import com.assignment.spring.exception.GenericApiException;
import com.assignment.spring.persistence.entities.WeatherEntity;
import com.assignment.spring.persistence.repositories.WeatherRepository;
import com.assignment.spring.service.OpenWeatherMapService;
import com.assignment.spring.service.WeatherService;
import com.assignment.spring.webclient.model.WeatherApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Log4j2
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final OpenWeatherMapService openWeatherMapService;
    private final WeatherRepository weatherRepository;

    @Override
    public Mono<ResponseEntity<Object>> retrieveAndSaveWeather(String city) {

        return openWeatherMapService.fetchWeather(city)
                .flatMap(weatherApiResponse -> {

                    log.debug("Preparing to persist weather data");

                    WeatherEntity weatherData = mapWeatherResponseToWeatherEntity(weatherApiResponse);
                    if (weatherData == null) {
                        return Mono.error(new GenericApiException("5001", "Cannot read weather API response"));
                    }

                    weatherData = persistWeatherData(weatherData);
                    if (weatherData == null) {
                        return Mono.error(new GenericApiException("5002", "Cannot persist weather data"));
                    }

                    WeatherResponse weatherResponse = mapWeatherEntityToWeatherDto(weatherData);
                    // weatherDto cannot be null here

                    log.info("Weather data persisted: " + weatherData);

                    return Mono.just(ResponseEntity
                            .ok()
                            .body((Object) weatherResponse));
                })
                .onErrorResume(throwable -> {

                    log.error("Error while processing request. Stacktrace: ", throwable);

                    if (throwable instanceof GenericApiException) {
                        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                                ((GenericApiException) throwable).getErrorCode(), throwable.getMessage());
                    }
                    if (throwable instanceof CityNotFoundException) {
                        return buildErrorResponse(HttpStatus.NOT_FOUND, "4041", "City not found");
                    }
                    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                            "5009", "Error while processing request");
                });
    }

    public WeatherEntity mapWeatherResponseToWeatherEntity(WeatherApiResponse weatherAPIResponse) {
        WeatherEntity weatherData = null;
        try {
            weatherData = WeatherEntity.builder()
                    .city(weatherAPIResponse.getName())
                    .country(weatherAPIResponse.getSys().getCountry())
                    .temperature(weatherAPIResponse.getMain().getTemp())
                    .build();
        } catch (Exception ex) {
            log.error("Cannot map weather response to weather entity", ex);
        }

        return weatherData;
    }

    public WeatherEntity persistWeatherData(WeatherEntity weatherData) {
        WeatherEntity weatherDataUpdated = null;
        try {
            weatherDataUpdated = weatherRepository.save(weatherData);
        } catch (Exception ex) {
            log.error("Error persisting weather data", ex);
        }

        return weatherDataUpdated;
    }

    public WeatherResponse mapWeatherEntityToWeatherDto(WeatherEntity weatherEntity) {
        return weatherEntity == null ? null : WeatherResponse.builder()
                .city(weatherEntity.getCity())
                .country(weatherEntity.getCountry())
                .temperature(weatherEntity.getTemperature())
                .timestamp(weatherEntity.getCreationDate())
                .build();
    }

    private Mono<ResponseEntity<Object>> buildErrorResponse(HttpStatus httpStatus, String code, String message) {
        return Mono.just(ResponseEntity
                .status(httpStatus)
                .body(ErrorResponse.builder()
                        .code(code)
                        .message(message)
                        .build()));
    }
}
