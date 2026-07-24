package com.nutritrack.food.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutritrack.food.domain.NutrientSource;
import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductNutrient;
import com.nutritrack.food.domain.ProductSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MicroEnrichmentGateTest {

  @Test
  void macrosOnlyProductNeedsEnrichment() {
    Product product = baseProduct();
    product.replaceNutrients(
        new ArrayList<>(
            List.of(nutrient(product, "protein", "1.0", "g", NutrientSource.OFF, false))));
    assertThat(MicroEnrichmentGate.needsEnrichment(product)).isTrue();
  }

  @Test
  void offZerosDoNotCountAsFilled() {
    Product product = baseProduct();
    List<ProductNutrient> nutrients = new ArrayList<>();
    nutrients.add(nutrient(product, "protein", "9", "g", NutrientSource.OFF, false));
    for (String code :
        List.of(
            "calcium",
            "iron",
            "magnesium",
            "potassium",
            "zinc",
            "vitamin_c",
            "vitamin_d")) {
      nutrients.add(nutrient(product, code, "0", "mg", NutrientSource.OFF, false));
    }
    product.replaceNutrients(nutrients);
    assertThat(MicroEnrichmentGate.needsEnrichment(product)).isTrue();
    assertThat(MicroEnrichmentGate.isFilled(nutrients.get(1))).isFalse();
  }

  @Test
  void estimatedMicrosCountAsFilled() {
    Product product = baseProduct();
    List<ProductNutrient> nutrients = new ArrayList<>();
    nutrients.add(nutrient(product, "protein", "9", "g", NutrientSource.OFF, false));
    for (String code :
        List.of(
            "calcium",
            "iron",
            "magnesium",
            "potassium",
            "zinc",
            "vitamin_c",
            "vitamin_d")) {
      nutrients.add(
          nutrient(product, code, "1", "mg", NutrientSource.NEVO_ESTIMATE, true));
    }
    product.replaceNutrients(nutrients);
    assertThat(MicroEnrichmentGate.needsEnrichment(product)).isFalse();
  }

  private static Product baseProduct() {
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setBarcode("8718452513673");
    product.setSource(ProductSource.OFF);
    product.setName("Franse Kwark Mager");
    return product;
  }

  private static ProductNutrient nutrient(
      Product product,
      String code,
      String amount,
      String unit,
      NutrientSource source,
      boolean estimated) {
    ProductNutrient pn = new ProductNutrient();
    pn.setProductId(product.getId());
    pn.setNutrientCode(code);
    pn.setAmountPer100g(new BigDecimal(amount));
    pn.setUnit(unit);
    pn.setSource(source);
    pn.setEstimated(estimated);
    return pn;
  }
}
