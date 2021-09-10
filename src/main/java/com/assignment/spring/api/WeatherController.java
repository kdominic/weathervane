package com.assignment.spring.api;

import com.assignment.spring.dto.ErrorResponse;
import com.assignment.spring.dto.WeatherResponse;
import com.assignment.spring.service.WeatherService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1")
@Tag(name = "Weather API", description = "Weather API for fetching and persisting data")
public class WeatherController {

    private final WeatherService weatherService;

    @CrossOrigin
    @GetMapping("/fetch-weather")
    @Timed(value = "fetch-weather.time", description = "Time for retrieving weather data")
    @Operation(description = "Fetch weather data and persists it to the database", parameters = {
            @Parameter(name = "city", in = ParameterIn.QUERY, required = true, description = "name of the city"),
            @Parameter(name = "${api-security.api-key-header-name}", in = ParameterIn.HEADER, required = true, description = "API Key")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Weather data was retrieved and persisted",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WeatherResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing city parameter", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Wrong API Key", content = @Content),
            @ApiResponse(responseCode = "404", description = "No data found for the specified city",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error while processing the request",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))})
    public Mono<ResponseEntity<Object>> retrieveWeather(@RequestParam(name = "city") Optional<String> city) {
        log.info("Received request for /fetch-weather: " + city);
        if (!city.isPresent()) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .code("4001")
                            .message("Missing city parameter")
                            .build()));
        }

        return weatherService.retrieveAndSaveWeather(city.get());
    }
}
