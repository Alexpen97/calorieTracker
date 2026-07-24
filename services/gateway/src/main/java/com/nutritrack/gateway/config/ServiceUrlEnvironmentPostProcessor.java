package com.nutritrack.gateway.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Railway operators often set private service URLs without an {@code http://} scheme (e.g.
 * {@code diary-service.railway.internal:8080}). Java URI parsing treats that as a scheme, not a
 * host, which breaks Spring Cloud Gateway with "Host is not specified".
 */
public class ServiceUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

  static final String PROPERTY_SOURCE_NAME = "normalizedServiceUrls";

  private static final List<String> SERVICE_URL_VARS =
      List.of(
          "AUTH_SERVICE_URL",
          "USER_SERVICE_URL",
          "FOOD_SERVICE_URL",
          "DIARY_SERVICE_URL",
          "NEVO_SERVICE_URL",
          "RECO_SERVICE_URL",
          "JWKS_URI");

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    Map<String, Object> normalized = new HashMap<>();
    for (String property : SERVICE_URL_VARS) {
      String raw = environment.getProperty(property);
      if (raw == null || raw.isBlank()) {
        continue;
      }
      normalized.put(property, ServiceUrlNormalizer.normalize(raw));
    }
    if (!normalized.isEmpty()) {
      environment
          .getPropertySources()
          .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, normalized));
    }
  }
}
