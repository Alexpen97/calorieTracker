package com.nutritrack.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutritrack.gateway.config.ServiceUrlEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;

class ServiceUrlEnvironmentPostProcessorTest {

  @Test
  void normalizesBarePrivateDomainBeforeRoutesLoad() {
    MockPropertySource source = new MockPropertySource();
    source.setProperty("DIARY_SERVICE_URL", "diary-service.railway.internal:8080");
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().addLast(source);

    new ServiceUrlEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("DIARY_SERVICE_URL"))
        .isEqualTo("http://diary-service.railway.internal:8080");
  }
}
