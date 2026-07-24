package com.nutritrack.food.off;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record NormalizedOffProduct(
    String barcode,
    String name,
    String brand,
    String genericName,
    String quantityLabel,
    BigDecimal servingSizeG,
    String imageUrl,
    String nutriScore,
    String ingredientsText,
    List<String> allergenTags,
    List<NormalizedNutrient> nutrients) {

  public record NormalizedNutrient(String code, BigDecimal amountPer100g, String unit) {}
}
