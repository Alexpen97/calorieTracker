package com.nutritrack.food.web.dto;

import java.math.BigDecimal;

public record ProductNutrientResponse(String code, BigDecimal amountPer100g, String unit) {}
