package com.nutritrack.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutritrack.auth.config.AuthProperties;
import com.nutritrack.auth.config.AuthStartupValidator;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AuthStartupValidatorTest {

  @Test
  void skipsValidationInDevMode() {
    AuthStartupValidator validator = validator(devProperties());

    assertThatCode(validator::validateProductionConfig).doesNotThrowAnyException();
  }

  @Test
  void prodModeRejectsLocalUserServiceUrl() {
    AuthStartupValidator validator =
        validator(
            new AuthProperties(
                "prod",
                "client-id",
                "client-secret",
                "https://oauth2.googleapis.com/token",
                "https://www.googleapis.com/oauth2/v3/certs",
                "http://auth-service",
                "",
                "nutritrack-1",
                Duration.ofMinutes(15),
                Duration.ofDays(14),
                "http://localhost:8082",
                "prod-internal-key"));

    assertThatThrownBy(validator::validateProductionConfig)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("USER_SERVICE_URL");
  }

  private static AuthStartupValidator validator(AuthProperties properties) {
    return new AuthStartupValidator(properties);
  }

  private static AuthProperties devProperties() {
    return new AuthProperties(
        "dev",
        "",
        "",
        "https://oauth2.googleapis.com/token",
        "https://www.googleapis.com/oauth2/v3/certs",
        "http://auth-service",
        "",
        "nutritrack-1",
        Duration.ofMinutes(15),
        Duration.ofDays(14),
        "http://localhost:8082",
        "dev-internal-key");
  }
}
