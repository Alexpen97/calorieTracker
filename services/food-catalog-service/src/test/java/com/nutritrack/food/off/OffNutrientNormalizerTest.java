package com.nutritrack.food.off;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OffNutrientNormalizerTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void normalizesCommonOffNutrimentsAndServingSize() throws Exception {
    String json =
        """
        {
          "product_name": "Nutella",
          "brands": "Ferrero",
          "quantity": "400 g",
          "serving_size": "15 g",
          "nutrition_grades": "e",
          "ingredients_text": "sugar, palm oil",
          "allergens_tags": ["en:milk", "en:nuts"],
          "image_url": "https://example.test/nutella.jpg",
          "nutriments": {
            "energy-kcal_100g": 539,
            "proteins_100g": 6.3,
            "fat_100g": 30.9,
            "saturated-fat_100g": 10.6,
            "carbohydrates_100g": 57.5,
            "sugars_100g": 56.3,
            "fiber_100g": 0,
            "salt_100g": 0.107,
            "sodium_100g": 0.0428,
            "calcium_100g": 84
          }
        }
        """;
    JsonNode product = mapper.readTree(json);
    NormalizedOffProduct normalized = OffNutrientNormalizer.normalize("3017620422003", product);

    assertThat(normalized.name()).isEqualTo("Nutella");
    assertThat(normalized.brand()).isEqualTo("Ferrero");
    assertThat(normalized.servingSizeG()).isEqualByComparingTo("15");
    assertThat(normalized.nutriScore()).isEqualTo("E");
    assertThat(normalized.allergenTags()).containsExactly("en:milk", "en:nuts");
    assertThat(normalized.nutrients())
        .anySatisfy(
            n -> {
              assertThat(n.code()).isEqualTo("energy_kcal");
              assertThat(n.amountPer100g()).isEqualByComparingTo("539");
              assertThat(n.unit()).isEqualTo("kcal");
            })
        .anySatisfy(
            n -> {
              assertThat(n.code()).isEqualTo("sodium");
              assertThat(n.amountPer100g()).isEqualByComparingTo("42.8");
              assertThat(n.unit()).isEqualTo("mg");
            })
        .anySatisfy(
            n -> {
              assertThat(n.code()).isEqualTo("protein");
              assertThat(n.amountPer100g()).isEqualByComparingTo("6.3");
            });
  }
}
