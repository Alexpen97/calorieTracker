package com.nutritrack.diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutritrack.diary.config.DiaryUrlEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;

class DiaryUrlEnvironmentPostProcessorTest {

  @Test
  void stripsTrailingEqualsFromJwksUri() {
    MockPropertySource source = new MockPropertySource();
    source.setProperty("JWKS_URI", "http://auth.railway.internal:8080/.well-known/jwks.json=");
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().addLast(source);

    new DiaryUrlEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("JWKS_URI"))
        .isEqualTo("http://auth.railway.internal:8080/.well-known/jwks.json");
  }
}
