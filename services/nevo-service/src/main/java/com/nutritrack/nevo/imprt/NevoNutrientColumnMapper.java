package com.nutritrack.nevo.imprt;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Maps NEVO-online CSV column headers to NutriTrack nutrient codes. */
public final class NevoNutrientColumnMapper {

  private static final Map<String, Mapping> BY_NORMALIZED_HEADER = build();

  private NevoNutrientColumnMapper() {}

  public static Optional<Mapping> map(String header) {
    if (header == null || header.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(BY_NORMALIZED_HEADER.get(normalizeHeader(header)));
  }

  public static String normalizeHeader(String header) {
    return header
        .trim()
        .toLowerCase(Locale.ROOT)
        .replace('\u00a0', ' ')
        .replaceAll("\\s+", " ");
  }

  private static Map<String, Mapping> build() {
    Map<String, Mapping> map = new LinkedHashMap<>();
    put(map, "kcal (kcal)", "energy_kcal", "kcal");
    put(map, "kj (kj)", "energy_kj", "kJ");
    put(map, "protein (g)", "protein", "g");
    put(map, "fat (g)", "fat", "g");
    put(map, "sfa (g)", "saturated_fat", "g");
    put(map, "carbohydrate (g)", "carbohydrates", "g");
    put(map, "sugars (g)", "sugars", "g");
    put(map, "fibre (g)", "fiber", "g");
    put(map, "sodium (mg)", "sodium", "mg");
    put(map, "potassium (mg)", "potassium", "mg");
    put(map, "calcium (mg)", "calcium", "mg");
    put(map, "phosphorus (mg)", "phosphorus", "mg");
    put(map, "magnesium (mg)", "magnesium", "mg");
    put(map, "iron (mg)", "iron", "mg");
    put(map, "copper (mg)", "copper", "mg");
    put(map, "selenium (µg)", "selenium", "µg");
    put(map, "selenium (ug)", "selenium", "µg");
    put(map, "zinc (mg)", "zinc", "mg");
    put(map, "iodine (µg)", "iodine", "µg");
    put(map, "iodine (ug)", "iodine", "µg");
    put(map, "manganese (mg)", "manganese", "mg");
    put(map, "chromium (µg)", "chromium", "µg");
    put(map, "chromium (ug)", "chromium", "µg");
    put(map, "molybdenum (µg)", "molybdenum", "µg");
    put(map, "molybdenum (ug)", "molybdenum", "µg");
    put(map, "rae (vit a) (µg)", "vitamin_a", "µg");
    put(map, "rae (vit a) (ug)", "vitamin_a", "µg");
    put(map, "re (vit a) (µg)", "vitamin_a", "µg");
    put(map, "vit d (µg)", "vitamin_d", "µg");
    put(map, "vit d (ug)", "vitamin_d", "µg");
    put(map, "vit e (mg)", "vitamin_e", "mg");
    put(map, "vit k (µg)", "vitamin_k", "µg");
    put(map, "vit k (ug)", "vitamin_k", "µg");
    put(map, "vit b1 (mg)", "vitamin_b1", "mg");
    put(map, "vit b2 (mg)", "vitamin_b2", "mg");
    put(map, "niacin equiv (mg)", "vitamin_b3", "mg");
    put(map, "niacin (mg)", "vitamin_b3", "mg");
    put(map, "vit b6 (mg)", "vitamin_b6", "mg");
    put(map, "folate dfe (µg)", "vitamin_b9", "µg");
    put(map, "folate dfe (ug)", "vitamin_b9", "µg");
    put(map, "folate food (µg)", "vitamin_b9", "µg");
    put(map, "vit b12 (µg)", "vitamin_b12", "µg");
    put(map, "vit b12 (ug)", "vitamin_b12", "µg");
    put(map, "vit c (mg)", "vitamin_c", "mg");
    return Map.copyOf(map);
  }

  private static void put(Map<String, Mapping> map, String header, String code, String unit) {
    map.putIfAbsent(normalizeHeader(header), new Mapping(code, unit));
  }

  public static Optional<BigDecimal> parseAmount(String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("NA") || trimmed.equals("-")) {
      return Optional.empty();
    }
    String normalized = trimmed.replace(',', '.');
    try {
      return Optional.of(new BigDecimal(normalized));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  public record Mapping(String nutrientCode, String unit) {}
}
