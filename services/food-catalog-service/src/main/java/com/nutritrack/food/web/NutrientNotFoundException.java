package com.nutritrack.food.web;

public class NutrientNotFoundException extends RuntimeException {
  public NutrientNotFoundException(String code) {
    super("Nutrient not found: " + code);
  }
}
