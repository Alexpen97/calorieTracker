package com.nutritrack.enrichment.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameNormalizerTest {

  @Test
  void normalizeStripsPunctuationQuantitiesAndLowercases() {
    assertThat(NameNormalizer.normalize("Nutella Hazelnut Spread, 400 g"))
        .isEqualTo("nutella hazelnut spread");
  }

  @Test
  void tokenJaccardScoresOverlap() {
    assertThat(NameNormalizer.tokenJaccard("whole milk", "Milk, whole")).isEqualTo(1.0);
    assertThat(NameNormalizer.tokenJaccard("whole milk", "skim milk")).isGreaterThan(0.3);
    assertThat(NameNormalizer.tokenJaccard("apple", "orange")).isEqualTo(0.0);
  }

  @Test
  void looksFortifiedDetectsMarkers() {
    assertThat(NameNormalizer.looksFortified("Fortified breakfast cereal")).isTrue();
    assertThat(NameNormalizer.looksFortified("Oat drink unsweetened")).isTrue();
    assertThat(NameNormalizer.looksFortified("Whole milk")).isFalse();
  }

  @Test
  void stripLeadingZeros() {
    assertThat(NameNormalizer.stripLeadingZeros("000123")).isEqualTo("123");
    assertThat(NameNormalizer.stripLeadingZeros("0")).isEqualTo("0");
  }
}
