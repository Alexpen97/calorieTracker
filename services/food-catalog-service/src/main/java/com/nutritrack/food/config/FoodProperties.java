package com.nutritrack.food.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "nutritrack.food")
public record FoodProperties(
    @DefaultValue Off off, @DefaultValue Cache cache, @DefaultValue Resilience resilience) {

  public record Off(
      @DefaultValue("https://world.openfoodfacts.org") String baseUrl,
      @DefaultValue("NutriTrack - Server - Version 0.1") String userAgent,
      @DefaultValue(
              "product_name,generic_name,brands,nutriments,nutrition_grades,nutriscore_grade,ingredients_text,allergens_tags,serving_size,quantity,image_url,image_front_url")
          String fields) {}

  public record Cache(@DefaultValue("true") boolean redisEnabled, @DefaultValue("24h") Duration ttl) {}

  public record Resilience(
      @DefaultValue("12") int rateLimitPerMinute,
      @DefaultValue("30s") Duration circuitWaitDuration,
      @DefaultValue("2") int retryMaxAttempts) {}
}
