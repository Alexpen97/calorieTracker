package com.nutritrack.enrichment.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record EnrichResponse(
    String matchType,
    Long fdcId,
    String matchedDescription,
    BigDecimal confidence,
    List<NutrientDto> nutrients) {

  public record NutrientDto(String code, BigDecimal amountPer100g, String unit) {}
}
