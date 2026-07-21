package com.nutritrack.diary.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PortionMathTest {

  @Test
  void scalesPer100gNutrientByPortionWeight() {
    BigDecimal amount = PortionMath.scale(new BigDecimal("200"), new BigDecimal("50"));

    assertThat(amount).isEqualByComparingTo(new BigDecimal("100.00"));
    assertThat(amount.scale()).isEqualTo(2);
  }
}
