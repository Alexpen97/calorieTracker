package com.nutritrack.food.nevo;

import java.util.List;

public record NevoFoodSearchResponse(String query, List<Item> items) {

  public NevoFoodSearchResponse {
    items = items == null ? List.of() : List.copyOf(items);
  }

  public record Item(
      String nevoCode,
      String nameEn,
      String nameNl,
      String foodGroup,
      String synonym,
      List<NevoMatchResponse.NevoNutrientDto> nutrients) {

    public Item {
      nutrients = nutrients == null ? List.of() : List.copyOf(nutrients);
    }
  }
}
