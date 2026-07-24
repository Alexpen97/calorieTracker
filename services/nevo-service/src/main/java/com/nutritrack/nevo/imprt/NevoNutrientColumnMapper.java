package com.nutritrack.nevo.imprt;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps NEVO-online 2025/9.0 nutrient column headers (e.g. {@code ENERCC (kcal)}, {@code
 * VITA_RAE (µg)}) to NutriTrack nutrient codes.
 */
public final class NevoNutrientColumnMapper {

  private static final Pattern CODE_UNIT =
      Pattern.compile("^([A-Za-z0-9:_]+)\\s*\\(([^)]+)\\)\\s*$");

  private static final Map<String, Mapping> BY_NEVO_CODE = build();

  private NevoNutrientColumnMapper() {}

  public static Optional<Mapping> map(String header) {
    if (header == null || header.isBlank()) {
      return Optional.empty();
    }
    String trimmed = header.trim().replace("\"", "");
    Matcher matcher = CODE_UNIT.matcher(trimmed);
    if (matcher.matches()) {
      String nevoCode = matcher.group(1).toUpperCase(Locale.ROOT);
      return Optional.ofNullable(BY_NEVO_CODE.get(nevoCode));
    }
    // Fallback: bare nutrient code without unit suffix.
    return Optional.ofNullable(BY_NEVO_CODE.get(trimmed.toUpperCase(Locale.ROOT)));
  }

  public static Optional<BigDecimal> parseAmount(String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    String trimmed = raw.trim().replace("\"", "");
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

  private static Map<String, Mapping> build() {
    Map<String, Mapping> map = new LinkedHashMap<>();
    put(map, "ENERCC", "energy_kcal", "kcal");
    put(map, "ENERCJ", "energy_kj", "kJ");
    put(map, "PROT", "protein", "g");
    put(map, "FAT", "fat", "g");
    put(map, "FASAT", "saturated_fat", "g");
    put(map, "CHO", "carbohydrates", "g");
    put(map, "SUGAR", "sugars", "g");
    put(map, "FIBT", "fiber", "g");
    put(map, "NA", "sodium", "mg");
    put(map, "K", "potassium", "mg");
    put(map, "CA", "calcium", "mg");
    put(map, "P", "phosphorus", "mg");
    put(map, "MG", "magnesium", "mg");
    put(map, "FE", "iron", "mg");
    put(map, "CU", "copper", "mg");
    put(map, "SE", "selenium", "µg");
    put(map, "ZN", "zinc", "mg");
    put(map, "ID", "iodine", "µg");
    // Prefer RAE over RE for vitamin A.
    put(map, "VITA_RAE", "vitamin_a", "µg");
    put(map, "VITA_RE", "vitamin_a", "µg");
    put(map, "VITD", "vitamin_d", "µg");
    put(map, "VITE", "vitamin_e", "mg");
    put(map, "VITK", "vitamin_k", "µg");
    put(map, "THIA", "vitamin_b1", "mg");
    put(map, "RIBF", "vitamin_b2", "mg");
    // Prefer niacin equivalents over plain niacin.
    put(map, "NIAEQ", "vitamin_b3", "mg");
    put(map, "NIA", "vitamin_b3", "mg");
    put(map, "VITB6", "vitamin_b6", "mg");
    // Prefer dietary folate equivalents.
    put(map, "FOL", "vitamin_b9", "µg");
    put(map, "FOLFD", "vitamin_b9", "µg");
    put(map, "VITB12", "vitamin_b12", "µg");
    put(map, "VITC", "vitamin_c", "mg");
    return Map.copyOf(map);
  }

  private static void put(Map<String, Mapping> map, String nevoCode, String code, String unit) {
    map.putIfAbsent(nevoCode, new Mapping(code, unit));
  }

  public record Mapping(String nutrientCode, String unit) {}
}
