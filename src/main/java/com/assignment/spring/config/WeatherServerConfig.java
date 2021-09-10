package com.assignment.spring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "weather-server")
public class WeatherServerConfig {
    private String urlBase;
    private String urlEp;
    private String appId;
    private Long timeout = 5000L;
    private Integer retries = 3;
}
