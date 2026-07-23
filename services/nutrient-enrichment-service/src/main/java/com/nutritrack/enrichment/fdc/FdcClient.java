package com.nutritrack.enrichment.fdc;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

public class FdcClient {

  private static final Logger log = LoggerFactory.getLogger(FdcClient.class);

  private final RestClient restClient;
  private final String apiKey;
  private final RateLimiter rateLimiter;
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;

  public FdcClient(
      RestClient restClient,
      String apiKey,
      RateLimiter rateLimiter,
      CircuitBreaker circuitBreaker,
      Retry retry) {
    this.restClient = restClient;
    this.apiKey = apiKey;
    this.rateLimiter = rateLimiter;
    this.circuitBreaker = circuitBreaker;
    this.retry = retry;
  }

  public List<FdcSearchHit> searchFoods(
      String query, List<String> dataTypes, String brandOwner, int pageSize) {
    Supplier<List<FdcSearchHit>> supplier =
        Decorators.ofSupplier(() -> doSearch(query, dataTypes, brandOwner, pageSize))
            .withRateLimiter(rateLimiter)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .decorate();
    try {
      return supplier.get();
    } catch (CallNotPermittedException | RequestNotPermitted ex) {
      log.warn("FDC search blocked by resilience: {}", ex.toString());
      return List.of();
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().is4xxClientError()) {
        log.debug("FDC search 4xx: {}", ex.getStatusCode());
        return List.of();
      }
      throw ex;
    }
  }

  public Optional<FdcFoodDetail> getFood(long fdcId) {
    Supplier<Optional<FdcFoodDetail>> supplier =
        Decorators.ofSupplier(() -> doGetFood(fdcId))
            .withRateLimiter(rateLimiter)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .decorate();
    try {
      return supplier.get();
    } catch (CallNotPermittedException | RequestNotPermitted ex) {
      log.warn("FDC food blocked by resilience: {}", ex.toString());
      return Optional.empty();
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().is4xxClientError()) {
        return Optional.empty();
      }
      throw ex;
    }
  }

  private List<FdcSearchHit> doSearch(
      String query, List<String> dataTypes, String brandOwner, int pageSize) {
    try {
      JsonNode body =
          restClient
              .get()
              .uri(
                  uriBuilder -> {
                    var builder =
                        uriBuilder
                            .path("/foods/search")
                            .queryParam("api_key", apiKey)
                            .queryParam("query", query)
                            .queryParam("pageSize", pageSize);
                    if (dataTypes != null) {
                      for (String type : dataTypes) {
                        builder = builder.queryParam("dataType", type);
                      }
                    }
                    if (brandOwner != null && !brandOwner.isBlank()) {
                      builder = builder.queryParam("brandOwner", brandOwner);
                    }
                    return builder.build();
                  })
              .retrieve()
              .body(JsonNode.class);
      if (body == null || !body.has("foods") || !body.get("foods").isArray()) {
        return List.of();
      }
      List<FdcSearchHit> hits = new ArrayList<>();
      for (JsonNode food : body.get("foods")) {
        Long id = longValue(food, "fdcId");
        if (id == null) {
          continue;
        }
        hits.add(
            new FdcSearchHit(
                id,
                text(food, "description"),
                text(food, "brandOwner"),
                text(food, "gtinUpc"),
                text(food, "dataType")));
      }
      return hits;
    } catch (RestClientResponseException ex) {
      HttpStatusCode code = ex.getStatusCode();
      if (code.value() == 404) {
        return List.of();
      }
      throw ex;
    }
  }

  private Optional<FdcFoodDetail> doGetFood(long fdcId) {
    try {
      JsonNode body =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/food/{fdcId}")
                          .queryParam("api_key", apiKey)
                          .queryParam("format", "full")
                          .build(fdcId))
              .retrieve()
              .body(JsonNode.class);
      if (body == null) {
        return Optional.empty();
      }
      Long id = longValue(body, "fdcId");
      if (id == null) {
        return Optional.empty();
      }
      return Optional.of(
          new FdcFoodDetail(
              id,
              text(body, "description"),
              text(body, "dataType"),
              FdcNutrientMapper.map(body.get("foodNutrients"))));
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 404) {
        return Optional.empty();
      }
      throw ex;
    }
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    String text = value.asString();
    return text == null || text.isBlank() ? null : text;
  }

  private static Long longValue(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull() || !value.isNumber()) {
      return null;
    }
    return value.asLong();
  }
}
