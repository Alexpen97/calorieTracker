package com.nutritrack.food.service;

import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductSubmission;
import com.nutritrack.food.domain.NutrientSource;
import com.nutritrack.food.web.dto.ProductNutrientResponse;
import com.nutritrack.food.web.dto.ProductResponse;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProductMapper {

  private final JsonMapper jsonMapper;

  public ProductMapper(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  public ProductResponse toResponse(Product product) {
    return new ProductResponse(
        product.getId(),
        null,
        product.getBarcode(),
        product.getSource().name(),
        product.getName(),
        product.getBrand(),
        product.getQuantityLabel(),
        ProductDensityResolver.resolve(
            product.getQuantityLabel(), product.getName(), product.getGenericName()),
        product.getServingSizeG(),
        product.getImageUrl(),
        product.getNutriScore(),
        product.getIngredientsText(),
        splitTags(product.getAllergenTags()),
        product.getOffLastSyncedAt(),
        product.getNutrients().stream()
            .map(
                n ->
                    new ProductNutrientResponse(
                        n.getNutrientCode(),
                        n.getAmountPer100g(),
                        n.getUnit(),
                        n.isEstimated()
                            || n.getSource() == NutrientSource.USDA_BRANDED
                            || n.getSource() == NutrientSource.USDA_PROXY
                            || n.getSource() == NutrientSource.NEVO_ESTIMATE))
            .toList());
  }

  public ProductResponse toResponse(ProductSubmission submission) {
    return new ProductResponse(
        submission.getId(),
        submission.getId(),
        submission.getBarcode(),
        "PENDING_SUBMISSION",
        submission.getName(),
        submission.getBrand(),
        null,
        null,
        submission.getServingSizeG(),
        null,
        null,
        null,
        List.of(),
        null,
        parseNutrients(submission.getNutrients()));
  }

  public String writeNutrients(List<ProductNutrientResponse> nutrients) {
    Map<String, Map<String, Object>> payload = new LinkedHashMap<>();
    for (ProductNutrientResponse nutrient : nutrients) {
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("amountPer100g", nutrient.amountPer100g());
      body.put("unit", nutrient.unit());
      body.put("estimated", nutrient.estimated());
      payload.put(nutrient.code(), body);
    }
    try {
      return jsonMapper.writeValueAsString(payload);
    } catch (JacksonException ex) {
      throw new IllegalArgumentException("Invalid nutrients payload", ex);
    }
  }

  public List<ProductNutrientResponse> parseNutrients(String json) {
    try {
      Map<String, Map<String, Object>> payload =
          jsonMapper.readValue(json, new TypeReference<>() {});
      return payload.entrySet().stream()
          .map(
              entry -> {
                Object amount = entry.getValue().get("amountPer100g");
                Object unit = entry.getValue().get("unit");
                Object estimated = entry.getValue().get("estimated");
                return new ProductNutrientResponse(
                    entry.getKey(),
                    amount instanceof BigDecimal bd
                        ? bd
                        : new BigDecimal(String.valueOf(amount)),
                    unit == null ? "g" : unit.toString(),
                    Boolean.TRUE.equals(estimated) || "true".equals(String.valueOf(estimated)));
              })
          .toList();
    } catch (JacksonException ex) {
      throw new IllegalStateException("Corrupt submission nutrients JSON", ex);
    }
  }

  private static List<String> splitTags(String allergenTags) {
    if (allergenTags == null || allergenTags.isBlank()) {
      return List.of();
    }
    return Arrays.stream(allergenTags.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }
}
