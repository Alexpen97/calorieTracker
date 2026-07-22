package com.nutritrack.food.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubmissionResponse(
    UUID id,
    UUID submitterUserId,
    String status,
    String barcode,
    String name,
    String brand,
    BigDecimal servingSizeG,
    List<ProductNutrientResponse> nutrients,
    Instant submittedAt,
    UUID reviewedBy,
    Instant reviewedAt,
    String reviewNote,
    UUID publishedProductId,
    List<String> duplicateWarnings) {}
