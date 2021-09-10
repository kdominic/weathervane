package com.assignment.spring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "api-security")
@Data
public class ApiSecurityConfig {
    private String apiKeyHeaderName;
    private String apiKeySecret;
}
