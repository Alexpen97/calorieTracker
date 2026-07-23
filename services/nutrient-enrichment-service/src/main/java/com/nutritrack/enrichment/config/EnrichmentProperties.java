package com.nutritrack.enrichment.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nutritrack.enrichment")
public record EnrichmentProperties(
    String internalApiKey, int cacheTtlDays, Fdc fdc, Resilience resilience) {

  public record Fdc(String baseUrl, String apiKey) {}

  public record Resilience(
      int rateLimitPerMinute, Duration circuitWaitDuration, int retryMaxAttempts) {}
}
