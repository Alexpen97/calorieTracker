package com.nutritrack.food.service;

import com.nutritrack.food.domain.NutrientSource;
import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductNutrient;
import com.nutritrack.food.domain.ProductSource;
import com.nutritrack.food.nevo.NevoMicronutrientCodes;
import com.nutritrack.food.web.dto.ProductNutrientResponse;
import com.nutritrack.food.web.dto.ProductResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Shared rules for when USDA/NEVO micro enrichment should run. */
final class MicroEnrichmentGate {

  static final int SPARSE_THRESHOLD = 6;

  private MicroEnrichmentGate() {}

  static boolean needsEnrichment(Product product) {
    if (product == null || product.getSource() != ProductSource.OFF) {
      return false;
    }
    Map<String, ProductNutrient> byCode = new HashMap<>();
    for (ProductNutrient nutrient : product.getNutrients()) {
      byCode.put(nutrient.getNutrientCode(), nutrient);
    }
    // Sparse = fewer than SPARSE_THRESHOLD filled micros (zeros / missing count as empty).
    return isSparse(byCode);
  }

  static boolean needsEnrichment(ProductResponse response) {
    if (response == null || !"OFF".equals(response.source())) {
      return false;
    }
    Map<String, ProductNutrientResponse> byCode = new HashMap<>();
    for (ProductNutrientResponse nutrient : response.nutrients()) {
      byCode.put(nutrient.code(), nutrient);
    }
    long meaningful =
        byCode.values().stream()
            .filter(n -> ProductEnrichmentService.MICRO_CODES.contains(n.code()))
            .filter(n -> positive(n.amountPer100g()) || n.estimated())
            .count();
    return meaningful < SPARSE_THRESHOLD;
  }

  static boolean isFilled(ProductNutrient nutrient) {
    if (nutrient == null) {
      return false;
    }
    NutrientSource source = nutrient.getSource();
    if (source == NutrientSource.USDA_BRANDED
        || source == NutrientSource.USDA_PROXY
        || source == NutrientSource.NEVO_ESTIMATE
        || source == NutrientSource.USER) {
      return true;
    }
    return positive(nutrient.getAmountPer100g());
  }

  static boolean isSparse(Map<String, ProductNutrient> byCode) {
    long meaningful =
        byCode.values().stream()
            .filter(n -> ProductEnrichmentService.MICRO_CODES.contains(n.getNutrientCode()))
            .filter(MicroEnrichmentGate::isFilled)
            .count();
    return meaningful < SPARSE_THRESHOLD;
  }

  static boolean hasNevoGaps(Map<String, ProductNutrient> byCode) {
    return NevoMicronutrientCodes.CODES.stream()
        .anyMatch(code -> !isFilled(byCode.get(code)));
  }

  /** Codes that enrichment providers must not overwrite. */
  static Set<String> filledCodes(Product product) {
    return product.getNutrients().stream()
        .filter(MicroEnrichmentGate::isFilled)
        .map(ProductNutrient::getNutrientCode)
        .collect(java.util.stream.Collectors.toSet());
  }

  private static boolean positive(BigDecimal amount) {
    return amount != null && amount.signum() > 0;
  }
}
