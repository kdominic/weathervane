package com.assignment.spring.security;

import com.assignment.spring.config.ApiSecurityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Log4j2
@RequiredArgsConstructor
public class ApiKeyFilter implements WebFilter {

    private final ApiSecurityConfig apiSecurityConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        final ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        if (!isApiRequest(request)) {
            return chain.filter(exchange);
        }

        List<String> apiKeyHeader;
        apiKeyHeader = request.getHeaders().get(apiSecurityConfig.getApiKeyHeaderName());

        if (apiKeyHeader == null || apiKeyHeader.isEmpty()) {
            log.error("Missing APIKey header");
            return rejectRequest(response, HttpStatus.UNAUTHORIZED);
        }

        final String apiKey = apiKeyHeader.get(0);

        if (!apiSecurityConfig.getApiKeySecret().equals(apiKey)) {
            log.error("Wrong APIKey");
            return rejectRequest(response, HttpStatus.UNAUTHORIZED);
        }

        return chain.filter(exchange);
    }

    private boolean isApiRequest(ServerHttpRequest request) {
        return request.getURI().getPath().contains("/api/");
    }

    private Mono<Void> rejectRequest(ServerHttpResponse response, HttpStatus httpStatus) {
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
}
