package com.nutritrack.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nutritrack.auth")
public record AuthProperties(
    String mode,
    String googleClientId,
    String googleClientSecret,
    String googleTokenUri,
    String googleJwksUri,
    String issuer,
    String jwtPrivateKeyPem,
    String jwtKeyId,
    Duration accessTokenTtl,
    Duration refreshTokenTtl,
    String userServiceUrl,
    String internalApiKey) {

  public boolean isDevMode() {
    return "dev".equalsIgnoreCase(mode);
  }
}
