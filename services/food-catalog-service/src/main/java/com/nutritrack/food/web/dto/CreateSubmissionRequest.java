package com.nutritrack.food.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record CreateSubmissionRequest(
    @NotBlank String name,
    String brand,
    String barcode,
    @Positive BigDecimal servingSizeG,
    @NotEmpty List<@Valid NutrientInput> nutrients,
    boolean force) {

  public record NutrientInput(
      @NotBlank String code,
      @NotNull @Positive BigDecimal amountPer100g,
      @NotBlank String unit) {}
}
