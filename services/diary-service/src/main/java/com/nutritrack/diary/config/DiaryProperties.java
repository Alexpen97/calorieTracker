package com.nutritrack.diary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nutritrack.diary")
public record DiaryProperties(String foodServiceUrl, String userServiceUrl) {}
