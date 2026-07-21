package com.nutritrack.auth.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AuthStartupValidator {

  private final AuthProperties properties;

  public AuthStartupValidator(AuthProperties properties) {
    this.properties = properties;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void validateProductionConfig() {
    if (properties.isDevMode()) {
      return;
    }

    if (isBlank(properties.googleClientId()) || isBlank(properties.googleClientSecret())) {
      throw new IllegalStateException(
          "AUTH_MODE=prod requires GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET on auth-service");
    }

    if (isLocalUserServiceUrl(properties.userServiceUrl())) {
      throw new IllegalStateException(
          "AUTH_MODE=prod requires USER_SERVICE_URL (e.g."
              + " http://user-profile-service.railway.internal:8080). The default"
              + " http://localhost:8082 only works in local docker-compose.");
    }

    if (isBlank(properties.internalApiKey()) || "dev-internal-key".equals(properties.internalApiKey())) {
      throw new IllegalStateException(
          "AUTH_MODE=prod requires INTERNAL_API_KEY on auth-service (same value as"
              + " user-profile-service)");
    }
  }

  private static boolean isLocalUserServiceUrl(String url) {
    if (url == null || url.isBlank()) {
      return true;
    }
    return url.contains("localhost") || url.contains("127.0.0.1");
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
