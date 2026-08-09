package com.nutritrack.food.service.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EntityManagerProductCandidateSearcherTest {

  private final SearchQueryNormalizer normalizer = new SearchQueryNormalizer();

  @Test
  void postgresQueryTextsIncludeSynonymSubstitutionsPerTokenGroup() {
    NormalizedQuery query = normalizer.normalize("oat milk");

    assertThat(EntityManagerProductCandidateSearcher.postgresQueryTexts(query))
        .containsExactly("oat milk", "oat drink");
  }

  @Test
  void postgresQueryTextsIncludeYogurtYoghurtSubstitution() {
    NormalizedQuery query = normalizer.normalize("greek yogurt");

    assertThat(EntityManagerProductCandidateSearcher.postgresQueryTexts(query))
        .containsExactly("greek yogurt", "greek yoghurt");
  }
}
