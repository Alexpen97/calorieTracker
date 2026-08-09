package com.nutritrack.food.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductDensityResolverTest {

  @Test
  void nullWhenNotVolumeCapable() {
    assertThat(ProductDensityResolver.resolve("400 g", "Nutella", null)).isNull();
    assertThat(ProductDensityResolver.resolve(null, "Water", null)).isNull();
  }

  @Test
  void defaultsToOneForBeverages() {
    assertThat(ProductDensityResolver.resolve("330 ml", "Coca-Cola", null))
        .isEqualByComparingTo("1.00");
  }

  @Test
  void oilHeuristicBelowOne() {
    assertThat(ProductDensityResolver.resolve("500 ml", "Olive oil", null))
        .isEqualByComparingTo("0.92");
    assertThat(ProductDensityResolver.resolve("1 L", "Zonnebloemolie", null))
        .isEqualByComparingTo("0.92");
  }

  @Test
  void honeyAndMilkHeuristics() {
    assertThat(ProductDensityResolver.resolve("350 g", "ignore", null)).isNull();
    assertThat(ProductDensityResolver.resolve("250 ml", "Honey", null))
        .isEqualByComparingTo("1.40");
    assertThat(ProductDensityResolver.resolve("1 L", "Semi-skimmed milk", "melk"))
        .isEqualByComparingTo("1.03");
  }

  @Test
  void derivesFromPackageWhenMassAndVolumePresent() {
    // 520 g / 500 ml = 1.04 g/ml
    assertThat(ProductDensityResolver.resolve("500 ml / 520 g", "Mystery drink", null))
        .isEqualByComparingTo("1.0400");
  }
}
