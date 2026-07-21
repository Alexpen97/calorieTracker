package com.nutritrack.diary.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PortionMath {

  private PortionMath() {}

  public static BigDecimal scale(BigDecimal amountPer100g, BigDecimal weightG) {
    return amountPer100g.multiply(weightG).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
  }
}
