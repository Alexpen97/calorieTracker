package com.nutritrack.food.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    UUID submissionId,
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
    List<ProductNutrientResponse> nutrients) {}
