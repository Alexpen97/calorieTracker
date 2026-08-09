package com.nutritrack.food.service.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EditDistanceTest {

  @Test
  void levenshteinNutelaNutellaIsOne() {
    assertThat(EditDistance.levenshtein("nutela", "nutella")).isEqualTo(1);
  }
}
