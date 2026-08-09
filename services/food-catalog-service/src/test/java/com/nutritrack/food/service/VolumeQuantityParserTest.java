package com.nutritrack.food.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VolumeQuantityParserTest {

  @Test
  void detectsVolumeUnitsAndNormalizesToMl() {
    assertThat(VolumeQuantityParser.hasVolumeUnit("330 ml")).isTrue();
    assertThat(VolumeQuantityParser.hasVolumeUnit("1 L")).isTrue();
    assertThat(VolumeQuantityParser.hasVolumeUnit("25 cl")).isTrue();
    assertThat(VolumeQuantityParser.hasVolumeUnit("2 litres")).isTrue();
    assertThat(VolumeQuantityParser.parseVolumeMl("330 ml")).hasValue(new BigDecimal("330"));
    assertThat(VolumeQuantityParser.parseVolumeMl("1 L")).hasValue(new BigDecimal("1000"));
    assertThat(VolumeQuantityParser.parseVolumeMl("25 cl")).hasValue(new BigDecimal("250"));
    assertThat(VolumeQuantityParser.parseVolumeMl("1.5 dl")).hasValue(new BigDecimal("150.0"));
  }

  @Test
  void rejectsMassOnlyOrNull() {
    assertThat(VolumeQuantityParser.hasVolumeUnit("400 g")).isFalse();
    assertThat(VolumeQuantityParser.hasVolumeUnit(null)).isFalse();
    assertThat(VolumeQuantityParser.hasVolumeUnit("")).isFalse();
    assertThat(VolumeQuantityParser.parseVolumeMl("400 g")).isEmpty();
  }

  @Test
  void parsesMassAndVolumeFromCompoundLabel() {
    assertThat(VolumeQuantityParser.parseVolumeMl("500 ml / 520 g")).hasValue(new BigDecimal("500"));
    assertThat(VolumeQuantityParser.parseMassG("500 ml / 520 g")).hasValue(new BigDecimal("520"));
    assertThat(VolumeQuantityParser.parseMassG("1 kg")).hasValue(new BigDecimal("1000"));
  }
}
