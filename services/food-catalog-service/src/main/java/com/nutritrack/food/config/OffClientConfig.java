package com.nutritrack.food.config;

import com.nutritrack.food.off.OffClient;
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
public class OffClientConfig {

  @Bean
  RestClient.Builder restClientBuilder() {
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(Duration.ofSeconds(8));
    return RestClient.builder().requestFactory(factory);
  }

  @Bean
  RateLimiter offRateLimiter(FoodProperties properties) {
    RateLimiterConfig config =
        RateLimiterConfig.custom()
            .limitForPeriod(properties.resilience().rateLimitPerMinute())
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
    return RateLimiter.of("offProductReads", config);
  }

  @Bean
  CircuitBreaker offCircuitBreaker(FoodProperties properties) {
    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50)
            .waitDurationInOpenState(properties.resilience().circuitWaitDuration())
            .build();
    return CircuitBreaker.of("offProductReads", config);
  }

  @Bean
  Retry offRetry(FoodProperties properties) {
    RetryConfig config =
        RetryConfig.custom()
            .maxAttempts(properties.resilience().retryMaxAttempts())
            .waitDuration(Duration.ofMillis(200))
            .build();
    return Retry.of("offProductReads", config);
  }

  @Bean
  OffClient offClient(
      RestClient.Builder restClientBuilder,
      FoodProperties properties,
      RateLimiter offRateLimiter,
      CircuitBreaker offCircuitBreaker,
      Retry offRetry) {
    RestClient client =
        restClientBuilder
            .baseUrl(properties.off().baseUrl())
            .defaultHeader("User-Agent", properties.off().userAgent())
            .build();
    return new OffClient(client, properties.off().fields(), offRateLimiter, offCircuitBreaker, offRetry);
  }
}
