package com.nutritrack.diary;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutritrack.diary.config.DiaryProperties;
import com.nutritrack.diary.config.DiaryStartupValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DiaryStartupValidatorTest {

  @Test
  void skipsWhenNotOnRailway() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", "http://localhost/jwks");
    env.setProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/diary");

    DiaryStartupValidator validator = new DiaryStartupValidator(env, properties());

    assertThatCode(validator::validateProductionConfig).doesNotThrowAnyException();
  }

  @Test
  void rejectsLocalJwksOnRailway() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("RAILWAY_ENVIRONMENT", "production");
    env.setProperty(
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri", "http://localhost:8081/.well-known/jwks.json");
    env.setProperty("spring.datasource.url", "jdbc:postgresql://postgres.railway.internal:5432/diary");

    DiaryStartupValidator validator = new DiaryStartupValidator(env, properties());

    assertThatThrownBy(validator::validateProductionConfig)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWKS_URI");
  }

  @Test
  void rejectsMissingDiaryDatabaseUrlOnRailway() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("RAILWAY_ENVIRONMENT", "production");
    env.setProperty(
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        "http://auth.railway.internal:8080/.well-known/jwks.json");
    env.setProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/diary");

    DiaryStartupValidator validator = new DiaryStartupValidator(env, properties());

    assertThatThrownBy(validator::validateProductionConfig)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SPRING_DATASOURCE_URL");
  }

  private static DiaryProperties properties() {
    return new DiaryProperties(
        "http://food.railway.internal:8080", "http://user.railway.internal:8080");
  }
}
