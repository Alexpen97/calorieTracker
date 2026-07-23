package com.nutritrack.enrichment.fdc;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class FdcNutrientMapperTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Test
  void mapsBrandedLabelMicrosAndSkipsIu() throws Exception {
    String json =
        new String(
            getClass().getResourceAsStream("/fdc/food-branded.json").readAllBytes(),
            StandardCharsets.UTF_8);
    JsonNode root = jsonMapper.readTree(json);

    var mapped = FdcNutrientMapper.map(root.get("foodNutrients"));

    assertThat(mapped)
        .extracting(MappedNutrient::code)
        .containsExactlyInAnyOrder("vitamin_b3", "calcium")
        .doesNotContain("vitamin_d");
  }

  @Test
  void mapsFoundationRichMicrosIncludingNameFallback() throws Exception {
    String json =
        new String(
            getClass().getResourceAsStream("/fdc/food-foundation.json").readAllBytes(),
            StandardCharsets.UTF_8);
    JsonNode root = jsonMapper.readTree(json);

    var mapped = FdcNutrientMapper.map(root.get("foodNutrients"));

    assertThat(mapped)
        .extracting(MappedNutrient::code)
        .contains(
            "vitamin_b1",
            "vitamin_b2",
            "vitamin_b3",
            "vitamin_c",
            "calcium",
            "iron",
            "chromium");
  }
}
