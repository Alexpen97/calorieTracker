package com.nutritrack.food.off;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

public final class OffNutrientNormalizer {

  private static final Pattern SERVING_GRAMS =
      Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*g", Pattern.CASE_INSENSITIVE);

  private static final Map<String, Mapping> MAPPINGS = buildMappings();

  private OffNutrientNormalizer() {}

  public static NormalizedOffProduct normalize(String barcode, JsonNode productNode) {
    String name = text(productNode, "product_name");
    if (name == null || name.isBlank()) {
      name = text(productNode, "generic_name");
    }
    if (name == null || name.isBlank()) {
      name = "Unknown product";
    }

    String brand = text(productNode, "brands");
    String quantity = text(productNode, "quantity");
    String imageUrl =
        firstNonBlank(text(productNode, "image_url"), text(productNode, "image_front_url"));
    String nutriScore =
        firstNonBlank(text(productNode, "nutrition_grades"), text(productNode, "nutriscore_grade"));
    if (nutriScore != null) {
      nutriScore = nutriScore.substring(0, 1).toUpperCase(Locale.ROOT);
    }
    String ingredients = text(productNode, "ingredients_text");
    List<String> allergens = tags(productNode.get("allergens_tags"));
    BigDecimal servingSizeG = parseServingGrams(text(productNode, "serving_size"));

    List<NormalizedOffProduct.NormalizedNutrient> nutrients = new ArrayList<>();
    JsonNode nutriments = productNode.get("nutriments");
    if (nutriments != null && nutriments.isObject()) {
      for (Map.Entry<String, Mapping> entry : MAPPINGS.entrySet()) {
        JsonNode valueNode = nutriments.get(entry.getKey());
        if (valueNode == null || valueNode.isNull() || !valueNode.isNumber()) {
          continue;
        }
        Mapping mapping = entry.getValue();
        BigDecimal amount = valueNode.decimalValue();
        if (mapping.scaleToMg()) {
          amount = amount.multiply(BigDecimal.valueOf(1000));
        }
        nutrients.add(
            new NormalizedOffProduct.NormalizedNutrient(mapping.code(), amount, mapping.unit()));
      }
    }

    return new NormalizedOffProduct(
        barcode,
        name.trim(),
        blankToNull(brand),
        blankToNull(quantity),
        servingSizeG,
        blankToNull(imageUrl),
        blankToNull(nutriScore),
        blankToNull(ingredients),
        allergens,
        nutrients);
  }

  static Optional<BigDecimal> readAmount(JsonNode nutriments, String offKey) {
    if (nutriments == null || !nutriments.has(offKey) || !nutriments.get(offKey).isNumber()) {
      return Optional.empty();
    }
    return Optional.of(nutriments.get(offKey).decimalValue());
  }

  private static Map<String, Mapping> buildMappings() {
    Map<String, Mapping> map = new LinkedHashMap<>();
    map.put("energy-kcal_100g", new Mapping("energy_kcal", "kcal", false));
    map.put("energy-kj_100g", new Mapping("energy_kj", "kJ", false));
    map.put("proteins_100g", new Mapping("protein", "g", false));
    map.put("fat_100g", new Mapping("fat", "g", false));
    map.put("saturated-fat_100g", new Mapping("saturated_fat", "g", false));
    map.put("carbohydrates_100g", new Mapping("carbohydrates", "g", false));
    map.put("sugars_100g", new Mapping("sugars", "g", false));
    map.put("fiber_100g", new Mapping("fiber", "g", false));
    map.put("salt_100g", new Mapping("salt", "g", false));
    // OFF stores sodium in grams; internal model uses mg.
    map.put("sodium_100g", new Mapping("sodium", "mg", true));
    map.put("vitamin-a_100g", new Mapping("vitamin_a", "µg", false));
    map.put("vitamin-b1_100g", new Mapping("vitamin_b1", "mg", false));
    map.put("vitamin-b2_100g", new Mapping("vitamin_b2", "mg", false));
    map.put("vitamin-pp_100g", new Mapping("vitamin_b3", "mg", false));
    map.put("pantothenic-acid_100g", new Mapping("vitamin_b5", "mg", false));
    map.put("vitamin-b6_100g", new Mapping("vitamin_b6", "mg", false));
    map.put("biotin_100g", new Mapping("vitamin_b7", "µg", false));
    map.put("vitamin-b9_100g", new Mapping("vitamin_b9", "µg", false));
    map.put("vitamin-b12_100g", new Mapping("vitamin_b12", "µg", false));
    map.put("vitamin-c_100g", new Mapping("vitamin_c", "mg", false));
    map.put("vitamin-d_100g", new Mapping("vitamin_d", "µg", false));
    map.put("vitamin-e_100g", new Mapping("vitamin_e", "mg", false));
    map.put("vitamin-k_100g", new Mapping("vitamin_k", "µg", false));
    map.put("calcium_100g", new Mapping("calcium", "mg", false));
    map.put("iron_100g", new Mapping("iron", "mg", false));
    map.put("magnesium_100g", new Mapping("magnesium", "mg", false));
    map.put("potassium_100g", new Mapping("potassium", "mg", false));
    map.put("zinc_100g", new Mapping("zinc", "mg", false));
    map.put("iodine_100g", new Mapping("iodine", "µg", false));
    map.put("selenium_100g", new Mapping("selenium", "µg", false));
    map.put("copper_100g", new Mapping("copper", "mg", false));
    map.put("manganese_100g", new Mapping("manganese", "mg", false));
    map.put("phosphorus_100g", new Mapping("phosphorus", "mg", false));
    map.put("chromium_100g", new Mapping("chromium", "µg", false));
    map.put("molybdenum_100g", new Mapping("molybdenum", "µg", false));
    return Map.copyOf(map);
  }

  private static BigDecimal parseServingGrams(String servingSize) {
    if (servingSize == null || servingSize.isBlank()) {
      return null;
    }
    Matcher matcher = SERVING_GRAMS.matcher(servingSize);
    if (!matcher.find()) {
      return null;
    }
    return new BigDecimal(matcher.group(1));
  }

  private static List<String> tags(JsonNode node) {
    if (node == null || !node.isArray()) {
      return List.of();
    }
    List<String> tags = new ArrayList<>();
    node.forEach(
        item -> {
          if (item.isString() && !item.asString().isBlank()) {
            tags.add(item.asString());
          }
        });
    return List.copyOf(tags);
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull() || !value.isString()) {
      return null;
    }
    return value.asString();
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record Mapping(String code, String unit, boolean scaleToMg) {}
}
