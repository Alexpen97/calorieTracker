package com.nutritrack.nevo.web.dto;

import java.util.List;

public record NevoFoodSearchResponse(String query, List<Item> items) {

  public record Item(
      String nevoCode,
      String nameEn,
      String nameNl,
      String foodGroup,
      String synonym,
      List<NevoMatchResponse.NevoNutrientDto> nutrients) {}
}
