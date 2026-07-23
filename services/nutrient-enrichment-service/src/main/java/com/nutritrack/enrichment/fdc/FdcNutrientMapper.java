package com.nutritrack.enrichment.fdc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public final class FdcNutrientMapper {

  private static final Map<String, Mapping> BY_NUMBER = buildByNumber();
  private static final Map<String, Mapping> BY_NAME = buildByName();
  private static final Set<String> ACCEPTED_UNITS = Set.of("mg", "µg", "ug", "g", "kcal", "kj");

  private FdcNutrientMapper() {}

  public static List<MappedNutrient> map(JsonNode foodNutrients) {
    List<MappedNutrient> out = new ArrayList<>();
    if (foodNutrients == null || !foodNutrients.isArray()) {
      return out;
    }
    for (JsonNode row : foodNutrients) {
      JsonNode nutrient = row.get("nutrient");
      if (nutrient == null || nutrient.isNull()) {
        continue;
      }
      String number = text(nutrient, "number");
      String name = text(nutrient, "name");
      String unit = text(nutrient, "unitName");
      if (unit == null) {
        unit = text(row, "unitName");
      }
      JsonNode amountNode = row.get("amount");
      if (amountNode == null || amountNode.isNull() || !amountNode.isNumber()) {
        continue;
      }
      Mapping mapping = null;
      if (number != null) {
        mapping = BY_NUMBER.get(number.trim());
      }
      if (mapping == null && name != null) {
        mapping = BY_NAME.get(name.trim().toLowerCase(Locale.ROOT));
      }
      if (mapping == null) {
        continue;
      }
      if (unit == null || !isCompatibleUnit(unit, mapping.unit())) {
        continue;
      }
      BigDecimal amount = amountNode.decimalValue();
      out.add(new MappedNutrient(mapping.code(), amount, mapping.unit()));
    }
    return out;
  }

  private static boolean isCompatibleUnit(String fdcUnit, String expected) {
    String normalized = fdcUnit.trim().toLowerCase(Locale.ROOT);
    if ("ug".equals(normalized)) {
      normalized = "µg";
    }
    if ("mcg".equals(normalized)) {
      normalized = "µg";
    }
    if (!ACCEPTED_UNITS.contains(normalized) && !"µg".equals(normalized)) {
      // IU and other non-convertible units are skipped
      return false;
    }
    String expectedNorm = expected.toLowerCase(Locale.ROOT);
    if ("ug".equals(expectedNorm)) {
      expectedNorm = "µg";
    }
    return normalized.equals(expectedNorm);
  }

  private static Map<String, Mapping> buildByNumber() {
    Map<String, Mapping> map = new LinkedHashMap<>();
    map.put("320", new Mapping("vitamin_a", "µg"));
    map.put("404", new Mapping("vitamin_b1", "mg"));
    map.put("405", new Mapping("vitamin_b2", "mg"));
    map.put("406", new Mapping("vitamin_b3", "mg"));
    map.put("410", new Mapping("vitamin_b5", "mg"));
    map.put("415", new Mapping("vitamin_b6", "mg"));
    map.put("416", new Mapping("vitamin_b7", "µg"));
    map.put("417", new Mapping("vitamin_b9", "µg"));
    map.put("418", new Mapping("vitamin_b12", "µg"));
    map.put("401", new Mapping("vitamin_c", "mg"));
    map.put("328", new Mapping("vitamin_d", "µg"));
    map.put("323", new Mapping("vitamin_e", "mg"));
    map.put("430", new Mapping("vitamin_k", "µg"));
    map.put("301", new Mapping("calcium", "mg"));
    map.put("303", new Mapping("iron", "mg"));
    map.put("304", new Mapping("magnesium", "mg"));
    map.put("305", new Mapping("phosphorus", "mg"));
    map.put("306", new Mapping("potassium", "mg"));
    map.put("307", new Mapping("sodium", "mg"));
    map.put("309", new Mapping("zinc", "mg"));
    map.put("312", new Mapping("copper", "mg"));
    map.put("314", new Mapping("iodine", "µg"));
    map.put("315", new Mapping("manganese", "mg"));
    map.put("317", new Mapping("selenium", "µg"));
    return map;
  }

  private static Map<String, Mapping> buildByName() {
    Map<String, Mapping> map = new LinkedHashMap<>();
    map.put("chromium, cr", new Mapping("chromium", "µg"));
    map.put("molybdenum, mo", new Mapping("molybdenum", "µg"));
    return map;
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    String text = value.asString();
    return text == null || text.isBlank() ? null : text;
  }

  private record Mapping(String code, String unit) {}
}
