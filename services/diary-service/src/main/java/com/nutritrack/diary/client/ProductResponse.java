package com.nutritrack.diary.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String barcode,
    String source,
    String name,
    String brand,
    String quantityLabel,
    BigDecimal servingSizeG,
    String imageUrl,
    String nutriScore,
    String ingredientsText,
    List<String> allergenTags,
    Instant offLastSyncedAt,
    List<NutrientResponse> nutrients) {

  public record NutrientResponse(String code, BigDecimal amountPer100g, String unit) {}
}
