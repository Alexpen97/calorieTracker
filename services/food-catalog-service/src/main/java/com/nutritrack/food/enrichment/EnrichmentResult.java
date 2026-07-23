package com.nutritrack.food.enrichment;

import java.math.BigDecimal;
import java.util.List;

public record EnrichmentResult(
    String matchType, Long fdcId, String matchedDescription, BigDecimal confidence, List<Nutrient> nutrients) {

  public record Nutrient(String code, BigDecimal amountPer100g, String unit) {}
}
