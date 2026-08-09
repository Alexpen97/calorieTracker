package com.nutritrack.food.service.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductRelevanceScorerTest {

  private SearchQueryNormalizer normalizer;
  private ProductRelevanceScorer scorer;

  @BeforeEach
  void setUp() {
    normalizer = new SearchQueryNormalizer();
    scorer = new ProductRelevanceScorer(0.35);
  }

  @Test
  void nutellaBeatsAlphabeticalDistractor() {
    var q = normalizer.normalize("nutella");
    double nutella = scorer.score(q, "Nutella", "Ferrero", "nutella  ferrero");
    double aaron = scorer.score(q, "Aaron's Nuts", "Acme", "aaron's nuts  acme");
    assertThat(nutella).isGreaterThan(aaron);
  }

  @Test
  void oatMilkMatchesOatDrinkViaSynonym() {
    var q = normalizer.normalize("oat milk");
    assertThat(scorer.score(q, "Oat Drink - Barista", "Oatly", "oat drink - barista oatly"))
        .isGreaterThan(0);
  }

  @Test
  void nutelaFuzzyScoresNutella() {
    var q = normalizer.normalize("nutela");
    assertThat(scorer.score(q, "Nutella", "Ferrero", "nutella ferrero")).isGreaterThan(0);
  }

  @Test
  void nutelaFuzzyRanksWholeNameAboveContainingDistractor() {
    var q = normalizer.normalize("nutela");
    double nutella = scorer.score(q, "Nutella", "Ferrero", "nutella ferrero");
    double distractor =
        scorer.score(q, "Aardvark Nutella Spread", "Acme", "aardvark nutella spread acme");

    assertThat(nutella).isGreaterThan(distractor);
  }
}
