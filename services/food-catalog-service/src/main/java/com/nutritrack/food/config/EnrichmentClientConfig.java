package com.nutritrack.food.config;

import com.nutritrack.food.enrichment.EnrichmentClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class EnrichmentClientConfig {

  @Bean
  CircuitBreaker enrichmentCircuitBreaker(FoodProperties properties) {
    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .build();
    return CircuitBreaker.of("enrichment", config);
  }

  @Bean
  EnrichmentClient enrichmentClient(
      FoodProperties properties, CircuitBreaker enrichmentCircuitBreaker) {
    FoodProperties.Enrichment enrichment = properties.enrichment();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(enrichment.timeout());
    RestClient client =
        RestClient.builder().baseUrl(enrichment.baseUrl()).requestFactory(factory).build();
    return new EnrichmentClient(
        client, enrichment.internalApiKey(), enrichment.enabled(), enrichmentCircuitBreaker);
  }
}
