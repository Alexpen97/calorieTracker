package com.nutritrack.food.enrichment;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class EnrichmentClient {

  private static final Logger log = LoggerFactory.getLogger(EnrichmentClient.class);

  private final RestClient restClient;
  private final String apiKey;
  private final boolean enabled;
  private final CircuitBreaker circuitBreaker;

  public EnrichmentClient(
      RestClient restClient, String apiKey, boolean enabled, CircuitBreaker circuitBreaker) {
    this.restClient = restClient;
    this.apiKey = apiKey;
    this.enabled = enabled;
    this.circuitBreaker = circuitBreaker;
  }

  public Optional<EnrichmentResult> enrich(
      String barcode, String name, String brand, List<String> existingCodes) {
    if (!enabled) {
      return Optional.empty();
    }
    Supplier<Optional<EnrichmentResult>> supplier =
        Decorators.ofSupplier(() -> doEnrich(barcode, name, brand, existingCodes))
            .withCircuitBreaker(circuitBreaker)
            .decorate();
    try {
      return supplier.get();
    } catch (CallNotPermittedException ex) {
      log.warn("Enrichment circuit open: {}", ex.toString());
      return Optional.empty();
    } catch (RuntimeException ex) {
      log.warn("Enrichment call failed for {}: {}", barcode, ex.toString());
      return Optional.empty();
    }
  }

  private Optional<EnrichmentResult> doEnrich(
      String barcode, String name, String brand, List<String> existingCodes) {
    try {
      EnrichmentResult body =
          restClient
              .post()
              .uri("/internal/enrich")
              .header("X-Internal-Api-Key", apiKey)
              .body(
                  new EnrichRequest(
                      barcode, name, brand, existingCodes == null ? List.of() : existingCodes))
              .retrieve()
              .body(EnrichmentResult.class);
      return Optional.ofNullable(body);
    } catch (RestClientException ex) {
      log.warn("Enrichment HTTP error for {}: {}", barcode, ex.toString());
      return Optional.empty();
    }
  }

  private record EnrichRequest(
      String barcode, String name, String brand, List<String> existingNutrientCodes) {}
}
