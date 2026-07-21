package com.nutritrack.food.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nutritrack.food")
public record FoodProperties(
    Off off, Cache cache, Resilience resilience) {

  public record Off(String baseUrl, String userAgent, String fields) {}

  public record Cache(boolean redisEnabled, Duration ttl) {}

  public record Resilience(
      int rateLimitPerMinute, Duration circuitWaitDuration, int retryMaxAttempts) {}
}
