package com.nutritrack.diary.config;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail fast on Railway when diary cannot validate JWTs or reach Postgres — otherwise
 * authenticated {@code /api/diary/**} calls return opaque gateway 500s while {@code /v3/api-docs}
 * (permitAll, no DB) still looks healthy.
 */
@Component
public class DiaryStartupValidator {

  private static final Logger log = LoggerFactory.getLogger(DiaryStartupValidator.class);

  private final Environment environment;
  private final DiaryProperties diaryProperties;

  public DiaryStartupValidator(Environment environment, DiaryProperties diaryProperties) {
    this.environment = environment;
    this.diaryProperties = diaryProperties;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void validateProductionConfig() {
    if (!isRailwayDeploy()) {
      return;
    }

    String jwks =
        environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
    log.info("diary-service JWKS jwk-set-uri={}", jwks);
    requireRemoteHttpUrl("JWKS_URI", jwks);

    String datasource = environment.getProperty("spring.datasource.url");
    log.info("diary-service datasource url={}", datasource);
    if (datasource == null || datasource.isBlank()) {
      throw new IllegalStateException(
          "SPRING_DATASOURCE_URL is missing on diary-service. Set"
              + " jdbc:postgresql://postgres.railway.internal:5432/diary");
    }
    if (datasource.contains("localhost") || datasource.contains("127.0.0.1")) {
      throw new IllegalStateException(
          "SPRING_DATASOURCE_URL points at localhost on Railway. Set"
              + " jdbc:postgresql://postgres.railway.internal:5432/diary (and create the diary"
              + " database if Postgres was provisioned before Phase 3)");
    }

    requireRemoteHttpUrl("FOOD_SERVICE_URL", diaryProperties.foodServiceUrl());
    requireRemoteHttpUrl("USER_SERVICE_URL", diaryProperties.userServiceUrl());
  }

  private void requireRemoteHttpUrl(String envVar, String url) {
    if (url == null || url.isBlank()) {
      throw new IllegalStateException(envVar + " is missing on diary-service");
    }
    URI uri = URI.create(url.trim());
    if (uri.getHost() == null || uri.getHost().isBlank()) {
      throw new IllegalStateException(
          envVar
              + " has no host (value="
              + url
              + "). Authenticated /api/diary/** will fail with gateway 500.");
    }
    if ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost())) {
      throw new IllegalStateException(
          envVar + " points at " + uri.getHost() + " on Railway — use the private service URL");
    }
  }

  private boolean isRailwayDeploy() {
    String railwayEnv = environment.getProperty("RAILWAY_ENVIRONMENT");
    return railwayEnv != null && !railwayEnv.isBlank();
  }
}
