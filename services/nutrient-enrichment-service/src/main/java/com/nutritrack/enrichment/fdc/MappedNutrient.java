package com.nutritrack.enrichment.fdc;

import java.math.BigDecimal;

public record MappedNutrient(String code, BigDecimal amountPer100g, String unit) {}
