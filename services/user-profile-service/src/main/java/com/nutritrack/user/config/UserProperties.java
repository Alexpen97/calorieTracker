package com.nutritrack.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nutritrack.user")
public record UserProperties(String internalApiKey) {}
