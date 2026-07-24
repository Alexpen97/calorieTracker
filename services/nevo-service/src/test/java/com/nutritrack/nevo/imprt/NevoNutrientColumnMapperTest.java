package com.nutritrack.nevo.imprt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class NevoNutrientColumnMapperTest {

  @Test
  void mapsNevoCodeHeadersToInternalCodes() {
    assertThat(NevoNutrientColumnMapper.map("THIA (mg)"))
        .contains(new NevoNutrientColumnMapper.Mapping("vitamin_b1", "mg"));
    assertThat(NevoNutrientColumnMapper.map("VITA_RAE (µg)"))
        .contains(new NevoNutrientColumnMapper.Mapping("vitamin_a", "µg"));
    assertThat(NevoNutrientColumnMapper.map("FOL (µg)"))
        .contains(new NevoNutrientColumnMapper.Mapping("vitamin_b9", "µg"));
    assertThat(NevoNutrientColumnMapper.map("NA (mg)"))
        .contains(new NevoNutrientColumnMapper.Mapping("sodium", "mg"));
    assertThat(NevoNutrientColumnMapper.map("SE (µg)"))
        .contains(new NevoNutrientColumnMapper.Mapping("selenium", "µg"));
    assertThat(NevoNutrientColumnMapper.map("ENERCC (kcal)"))
        .contains(new NevoNutrientColumnMapper.Mapping("energy_kcal", "kcal"));
    assertThat(NevoNutrientColumnMapper.map("NIAEQ (mg)"))
        .contains(new NevoNutrientColumnMapper.Mapping("vitamin_b3", "mg"));
  }

  @Test
  void parseAmountHandlesDutchCommaAndEmpty() {
    assertThat(NevoNutrientColumnMapper.parseAmount("0,12")).contains(new java.math.BigDecimal("0.12"));
    assertThat(NevoNutrientColumnMapper.parseAmount("\"14\"")).contains(new java.math.BigDecimal("14"));
    assertThat(NevoNutrientColumnMapper.parseAmount("")).isEqualTo(Optional.empty());
    assertThat(NevoNutrientColumnMapper.parseAmount("NA")).isEqualTo(Optional.empty());
  }
}
