package com.nutritrack.food.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;

public final class ProductDensityResolver {

  private ProductDensityResolver() {}

  public static BigDecimal resolve(String quantityLabel, String name, String genericName) {
    if (!VolumeQuantityParser.hasVolumeUnit(quantityLabel)) {
      return null;
    }
    Optional<BigDecimal> volumeMl = VolumeQuantityParser.parseVolumeMl(quantityLabel);
    Optional<BigDecimal> massG = VolumeQuantityParser.parseMassG(quantityLabel);
    if (volumeMl.isPresent()
        && massG.isPresent()
        && volumeMl.get().compareTo(BigDecimal.ZERO) > 0) {
      return massG.get().divide(volumeMl.get(), 4, RoundingMode.HALF_UP);
    }
    String haystack =
        ((name == null ? "" : name) + " " + (genericName == null ? "" : genericName))
            .toLowerCase(Locale.ROOT);
    if (containsAny(haystack, "olijfolie", "olive oil", "olie", "oil")) {
      return new BigDecimal("0.92");
    }
    if (containsAny(haystack, "honey", "honing", "syrup", "siroop")) {
      return new BigDecimal("1.40");
    }
    if (containsAny(haystack, "yoghurt", "yogurt", "milk", "melk")) {
      return new BigDecimal("1.03");
    }
    return new BigDecimal("1.00");
  }

  private static boolean containsAny(String haystack, String... needles) {
    for (String needle : needles) {
      if (haystack.contains(needle)) {
        return true;
      }
    }
    return false;
  }
}
