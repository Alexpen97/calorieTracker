package com.nutritrack.food.nevo;

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
}
