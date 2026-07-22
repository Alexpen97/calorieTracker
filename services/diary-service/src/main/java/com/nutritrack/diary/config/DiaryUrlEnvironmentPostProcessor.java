package com.nutritrack.diary.config;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Strips trailing {@code =} / quotes from service URLs (common Railway paste mistake) and adds
 * {@code http://} when the scheme is missing.
 */
public class DiaryUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final List<String> URL_VARS =
      List.of("JWKS_URI", "FOOD_SERVICE_URL", "USER_SERVICE_URL", "SPRING_DATASOURCE_URL");

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    Map<String, Object> normalized = new HashMap<>();
    for (String property : URL_VARS) {
      String raw = environment.getProperty(property);
      if (raw == null || raw.isBlank()) {
        continue;
      }
      if ("SPRING_DATASOURCE_URL".equals(property)) {
        normalized.put(property, stripJunk(raw.trim()));
        continue;
      }
      normalized.put(property, normalizeHttpUrl(raw));
    }
    if (!normalized.isEmpty()) {
      environment
          .getPropertySources()
          .addFirst(new MapPropertySource("normalizedDiaryUrls", normalized));
    }
  }

  static String normalizeHttpUrl(String raw) {
    String cleaned = stripJunk(raw.trim());
    if (cleaned.isEmpty()) {
      return cleaned;
    }
    if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
      cleaned = "http://" + cleaned;
    }
    URI uri = URI.create(cleaned);
    if (uri.getHost() == null || uri.getHost().isBlank()) {
      throw new IllegalArgumentException("URL has no host: " + raw);
    }
    return cleaned;
  }

  static String stripJunk(String value) {
    String cleaned = value;
    while (cleaned.length() >= 2
        && ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
            || (cleaned.startsWith("'") && cleaned.endsWith("'")))) {
      cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
    }
    while (cleaned.endsWith("=")) {
      cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
    }
    return cleaned;
  }
}
