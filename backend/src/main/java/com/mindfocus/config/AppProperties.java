package com.mindfocus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String zone, String corsOrigin, String mailFrom, Jwt jwt, Push push) {
    public record Jwt(String secret, long expirationMinutes) {}
    public record Push(String publicKey, String privateKey, String subject) {}
}
