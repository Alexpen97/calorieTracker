package com.nutritrack.nevo.imprt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class NevoNutrientColumnMapperTest {

  @Test
  void mapsCommonNevoHeadersToInternalCodes() {
    assertThat(NevoNutrientColumnMapper.map("Vit B1 (mg)"))
        .contains(new NevoNutrientColumnMapper.Mapping("vitamin_b1", "mg"));
    assertThat(NevoNutrientColumnMapper.map("RAE (Vit A) (µg)"))
        .contains(new NevoNutrientColumnMapper.Mapping("vitamin_a", "µg"));
    assertThat(NevoNutrientColumnMapper.map("Folate DFE (µg)"))
        .contains(new NevoNutrientColumnMapper.Mapping("vitamin_b9", "µg"));
    assertThat(NevoNutrientColumnMapper.map("Sodium (mg)"))
        .contains(new NevoNutrientColumnMapper.Mapping("sodium", "mg"));
    assertThat(NevoNutrientColumnMapper.map("Selenium (µg)"))
        .contains(new NevoNutrientColumnMapper.Mapping("selenium", "µg"));
  }

  @Test
  void parseAmountHandlesCommaAndEmpty() {
    assertThat(NevoNutrientColumnMapper.parseAmount("1,25")).contains(new java.math.BigDecimal("1.25"));
    assertThat(NevoNutrientColumnMapper.parseAmount("")).isEqualTo(Optional.empty());
    assertThat(NevoNutrientColumnMapper.parseAmount("NA")).isEqualTo(Optional.empty());
  }
}
