package com.nutritrack.enrichment.config;

import com.nutritrack.enrichment.fdc.FdcClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class FdcClientConfig {

  @Bean
  RestClient.Builder restClientBuilder() {
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(Duration.ofSeconds(8));
    return RestClient.builder().requestFactory(factory);
  }

  @Bean
  RateLimiter fdcRateLimiter(EnrichmentProperties properties) {
    RateLimiterConfig config =
        RateLimiterConfig.custom()
            .limitForPeriod(properties.resilience().rateLimitPerMinute())
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
    return RateLimiter.of("fdcReads", config);
  }

  @Bean
  CircuitBreaker fdcCircuitBreaker(EnrichmentProperties properties) {
    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50)
            .waitDurationInOpenState(properties.resilience().circuitWaitDuration())
            .build();
    return CircuitBreaker.of("fdcReads", config);
  }

  @Bean
  Retry fdcRetry(EnrichmentProperties properties) {
    RetryConfig config =
        RetryConfig.custom()
            .maxAttempts(properties.resilience().retryMaxAttempts())
            .waitDuration(Duration.ofMillis(200))
            .build();
    return Retry.of("fdcReads", config);
  }

  @Bean
  FdcClient fdcClient(
      RestClient.Builder restClientBuilder,
      EnrichmentProperties properties,
      RateLimiter fdcRateLimiter,
      CircuitBreaker fdcCircuitBreaker,
      Retry fdcRetry) {
    RestClient client =
        restClientBuilder
            .baseUrl(properties.fdc().baseUrl())
            .defaultHeader("Accept", "application/json")
            .build();
    return new FdcClient(client, properties.fdc().apiKey(), fdcRateLimiter, fdcCircuitBreaker, fdcRetry);
  }
}
