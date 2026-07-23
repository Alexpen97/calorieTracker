package com.nutritrack.food.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "nutritrack.food")
public record FoodProperties(
    @DefaultValue Off off,
    @DefaultValue Cache cache,
    @DefaultValue Resilience resilience,
    @DefaultValue Search search,
    @DefaultValue BulkImport bulkImport,
    @DefaultValue Enrichment enrichment) {

  public record Off(
      @DefaultValue("https://world.openfoodfacts.org") String baseUrl,
      @DefaultValue("NutriTrack - Server - Version 0.1") String userAgent,
      @DefaultValue(
              "product_name,generic_name,brands,nutriments,nutrition_grades,nutriscore_grade,ingredients_text,allergens_tags,serving_size,quantity,image_url,image_front_url")
          String fields,
      @DefaultValue("20") int searchPageSize) {}

  public record Cache(@DefaultValue("true") boolean redisEnabled, @DefaultValue("24h") Duration ttl) {}

  public record Resilience(
      @DefaultValue("12") int rateLimitPerMinute,
      @DefaultValue("8") int searchRateLimitPerMinute,
      @DefaultValue("30s") Duration circuitWaitDuration,
      @DefaultValue("2") int retryMaxAttempts) {}

  public record Search(
      @DefaultValue("20") int pageSize,
      @DefaultValue("5") int localMinResultsBeforeOffFallback) {}

  public record BulkImport(
      @DefaultValue("false") boolean enabled,
      @DefaultValue("0 30 3 * * *") String cron,
      @DefaultValue("") String defaultUrl) {}

  public record Enrichment(
      @DefaultValue("false") boolean enabled,
      @DefaultValue("http://localhost:8086") String baseUrl,
      @DefaultValue("dev-internal-key") String internalApiKey,
      @DefaultValue("3s") Duration timeout) {}
}
