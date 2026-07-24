package com.nutritrack.food.nevo;

public interface NevoClient {
  NevoMatchResponse matchBest(NevoMatchRequest request);
}
