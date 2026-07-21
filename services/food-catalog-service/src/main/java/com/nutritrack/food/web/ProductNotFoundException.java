package com.nutritrack.food.web;

public class ProductNotFoundException extends RuntimeException {
  public ProductNotFoundException(String key) {
    super("Product not found: " + key);
  }
}
