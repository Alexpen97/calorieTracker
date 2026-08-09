package com.nutritrack.food.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VolumeQuantityParser {

  private static final Pattern VOLUME =
      Pattern.compile(
          "([0-9]+(?:\\.[0-9]+)?)\\s*(ml|cl|dl|l|liters|litres|liter|litre)\\b",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern MASS =
      Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(kg|g)\\b", Pattern.CASE_INSENSITIVE);

  private VolumeQuantityParser() {}

  public static boolean hasVolumeUnit(String quantityLabel) {
    return quantityLabel != null && VOLUME.matcher(quantityLabel).find();
  }

  public static Optional<BigDecimal> parseVolumeMl(String quantityLabel) {
    if (quantityLabel == null || quantityLabel.isBlank()) {
      return Optional.empty();
    }
    Matcher m = VOLUME.matcher(quantityLabel);
    if (!m.find()) {
      return Optional.empty();
    }
    BigDecimal amount = new BigDecimal(m.group(1));
    String unit = m.group(2).toLowerCase(Locale.ROOT);
    return Optional.of(
        switch (unit) {
          case "l", "liter", "litre", "liters", "litres" -> amount.multiply(new BigDecimal("1000"));
          case "cl" -> amount.multiply(new BigDecimal("10"));
          case "dl" -> amount.multiply(new BigDecimal("100"));
          default -> amount; // ml
        });
  }

  public static Optional<BigDecimal> parseMassG(String quantityLabel) {
    if (quantityLabel == null || quantityLabel.isBlank()) {
      return Optional.empty();
    }
    Matcher m = MASS.matcher(quantityLabel);
    if (!m.find()) {
      return Optional.empty();
    }
    BigDecimal amount = new BigDecimal(m.group(1));
    String unit = m.group(2).toLowerCase(Locale.ROOT);
    return Optional.of("kg".equals(unit) ? amount.multiply(new BigDecimal("1000")) : amount);
  }
}
