package com.nutritrack.food.service.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchQueryNormalizerTest {

  @Test
  void collapsesWhitespaceLowercasesAndStripsPunctuation() {
    var n = new SearchQueryNormalizer().normalize("  Oat,  Milk!! ");
    assertThat(n.normalized()).isEqualTo("oat milk");
    assertThat(n.tokens()).containsExactly("oat", "milk");
  }

  @Test
  void expandsMilkDrinkSynonymsIntoExpandedTokens() {
    var n = new SearchQueryNormalizer().normalize("oat milk");
    assertThat(n.expandedTokens()).contains("oat", "milk", "drink");
  }
}
