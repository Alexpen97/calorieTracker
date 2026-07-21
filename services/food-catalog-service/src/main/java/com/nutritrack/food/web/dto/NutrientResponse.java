package com.nutritrack.food.web.dto;

public record NutrientResponse(
    String code,
    String displayName,
    String category,
    String defaultUnit,
    String description,
    String bodyEffects,
    String deficiencyEffects,
    String excessEffects,
    String commonSources,
    String contentSource) {}
