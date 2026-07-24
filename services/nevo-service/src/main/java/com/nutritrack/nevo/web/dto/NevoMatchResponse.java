package com.nutritrack.nevo.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record NevoMatchResponse(
    boolean matched,
    String nevoCode,
    String foodName,
    String foodGroup,
    String nevoVersion,
    String confidence,
    double score,
    List<String> reasons,
    List<NevoNutrientDto> nutrients) {

  public record NevoNutrientDto(String code, BigDecimal amountPer100g, String unit) {}

  public static NevoMatchResponse none() {
    return new NevoMatchResponse(
        false, null, null, null, null, "NONE", 0.0, List.of("no suitable NEVO match"), List.of());
  }
}
