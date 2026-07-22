package com.nutritrack.food.off;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public class OffClient {

  private final RestClient restClient;
  private final String fields;
  private final int searchPageSize;
  private final RateLimiter rateLimiter;
  private final RateLimiter searchRateLimiter;
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;

  public OffClient(
      RestClient restClient,
      String fields,
      int searchPageSize,
      RateLimiter rateLimiter,
      RateLimiter searchRateLimiter,
      CircuitBreaker circuitBreaker,
      Retry retry) {
    this.restClient = restClient;
    this.fields = fields;
    this.searchPageSize = searchPageSize;
    this.rateLimiter = rateLimiter;
    this.searchRateLimiter = searchRateLimiter;
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

  public List<NormalizedOffProduct> searchByName(String query, int page) {
    Supplier<List<NormalizedOffProduct>> supplier =
        Decorators.ofSupplier(() -> doSearch(query, page))
            .withRateLimiter(searchRateLimiter)
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

  private List<NormalizedOffProduct> doSearch(String query, int page) {
    try {
      JsonNode body =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/cgi/search.pl")
                          .queryParam("search_terms", query)
                          .queryParam("search_simple", 1)
                          .queryParam("action", "process")
                          .queryParam("json", 1)
                          .queryParam("page", Math.max(page, 1))
                          .queryParam("page_size", searchPageSize)
                          .build())
              .retrieve()
              .body(JsonNode.class);
      if (body == null || !body.has("products") || !body.get("products").isArray()) {
        return List.of();
      }
      List<NormalizedOffProduct> results = new ArrayList<>();
      for (JsonNode productNode : body.get("products")) {
        String code = text(productNode, "code");
        if (code == null || code.isBlank()) {
          continue;
        }
        results.add(OffNutrientNormalizer.normalize(code.trim(), productNode));
      }
      return results;
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 404) {
        return List.of();
      }
      throw ex;
    }
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    String text = value.asText();
    return text == null || text.isBlank() ? null : text;
  }
}
