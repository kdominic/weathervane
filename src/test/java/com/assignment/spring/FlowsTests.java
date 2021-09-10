package com.assignment.spring;

import com.assignment.spring.config.ApiSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FlowsTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ApiSecurityConfig apiSecurityConfig;

    @Test
    void happyFlow() {

        this.webTestClient
                .get()
                .uri("/api/v1/fetch-weather?city=London")
                .headers(headers -> headers.add(apiSecurityConfig.getApiKeyHeaderName(), apiSecurityConfig.getApiKeySecret()))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    void missingCityParameter() {

        this.webTestClient
                .get()
                .uri("/api/v1/fetch-weather")
                .headers(headers -> headers.add(apiSecurityConfig.getApiKeyHeaderName(), apiSecurityConfig.getApiKeySecret()))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void wrongApiKeyParameter() {

        this.webTestClient
                .get()
                .uri("/api/v1/fetch-weather?city=London")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unknownCityParameter() {

        this.webTestClient
                .get()
                .uri("/api/v1/fetch-weather?city=LondonX")
                .headers(headers -> headers.add(apiSecurityConfig.getApiKeyHeaderName(), apiSecurityConfig.getApiKeySecret()))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

}
