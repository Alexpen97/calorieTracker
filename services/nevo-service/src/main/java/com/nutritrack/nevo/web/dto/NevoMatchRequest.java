package com.nutritrack.nevo.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record NevoMatchRequest(
    String name,
    String brand,
    String genericName,
    List<String> categories,
    String ingredientsText,
    List<KnownMacro> knownMacros) {

  public record KnownMacro(String code, BigDecimal amountPer100g, String unit) {}
}
