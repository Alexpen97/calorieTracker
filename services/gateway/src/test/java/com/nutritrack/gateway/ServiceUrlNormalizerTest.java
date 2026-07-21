package com.nutritrack.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nutritrack.gateway.config.ServiceUrlNormalizer;
import org.junit.jupiter.api.Test;

class ServiceUrlNormalizerTest {

  @Test
  void addsHttpSchemeWhenMissing() {
    assertThat(ServiceUrlNormalizer.normalize("diary-service.railway.internal:8080"))
        .isEqualTo("http://diary-service.railway.internal:8080");
  }

  @Test
  void leavesValidHttpUrlUnchanged() {
    assertThat(ServiceUrlNormalizer.normalize("http://auth-service.railway.internal:8080"))
        .isEqualTo("http://auth-service.railway.internal:8080");
  }

  @Test
  void rejectsBlankHost() {
    assertThatThrownBy(() -> ServiceUrlNormalizer.normalize("http://"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("no host");
  }
}
