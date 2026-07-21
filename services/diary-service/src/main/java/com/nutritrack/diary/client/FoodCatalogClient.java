package com.nutritrack.diary.client;

import java.util.UUID;

public interface FoodCatalogClient {
  ProductResponse getProduct(UUID id, String bearerToken);
}
