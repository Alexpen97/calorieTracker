package com.nutritrack.food.nevo;

import java.util.List;

public interface NevoClient {
  NevoMatchResponse matchBest(NevoMatchRequest request);

  List<NevoFoodSearchResponse.Item> searchFoods(String q, int limit);
}
