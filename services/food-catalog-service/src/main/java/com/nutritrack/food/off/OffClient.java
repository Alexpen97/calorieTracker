package com.nutritrack.food.off;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public class OffClient {

  private final RestClient restClient;
  private final String fields;
  private final RateLimiter rateLimiter;
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;

  public OffClient(
      RestClient restClient,
      String fields,
      RateLimiter rateLimiter,
      CircuitBreaker circuitBreaker,
      Retry retry) {
    this.restClient = restClient;
    this.fields = fields;
    this.rateLimiter = rateLimiter;
    this.circuitBreaker = circuitBreaker;
    this.retry = retry;
  }

  public Optional<NormalizedOffProduct> fetchByBarcode(String barcode) {
    Supplier<Optional<NormalizedOffProduct>> supplier =
        Decorators.ofSupplier(() -> doFetch(barcode))
            .withRateLimiter(rateLimiter)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .decorate();
    return supplier.get();
  }

  private Optional<NormalizedOffProduct> doFetch(String barcode) {
    try {
      JsonNode body =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/api/v2/product/{barcode}")
                          .queryParam("fields", fields)
                          .build(barcode))
              .retrieve()
              .body(JsonNode.class);
      if (body == null) {
        return Optional.empty();
      }
      int status = body.path("status").asInt(0);
      if (status != 1 || !body.has("product") || body.get("product").isNull()) {
        return Optional.empty();
      }
      return Optional.of(OffNutrientNormalizer.normalize(barcode, body.get("product")));
    } catch (RestClientResponseException ex) {
      HttpStatusCode code = ex.getStatusCode();
      if (code.value() == 404) {
        return Optional.empty();
      }
      throw ex;
    }
  }
}
