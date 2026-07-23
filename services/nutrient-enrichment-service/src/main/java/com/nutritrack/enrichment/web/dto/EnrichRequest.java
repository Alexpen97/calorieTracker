package com.nutritrack.enrichment.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record EnrichRequest(
    @NotBlank String barcode,
    String name,
    String brand,
    List<String> existingNutrientCodes) {}
