package com.nutritrack.diary.client;

public class FoodCatalogUnavailableException extends RuntimeException {
  public FoodCatalogUnavailableException(Throwable cause) {
    super("Food catalog request failed", cause);
  }
}
