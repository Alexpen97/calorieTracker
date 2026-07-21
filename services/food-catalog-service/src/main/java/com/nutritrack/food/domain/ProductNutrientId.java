package com.nutritrack.food.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ProductNutrientId implements Serializable {

  private UUID productId;
  private String nutrientCode;

  public ProductNutrientId() {}

  public ProductNutrientId(UUID productId, String nutrientCode) {
    this.productId = productId;
    this.nutrientCode = nutrientCode;
  }

  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public String getNutrientCode() {
    return nutrientCode;
  }

  public void setNutrientCode(String nutrientCode) {
    this.nutrientCode = nutrientCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ProductNutrientId that)) {
      return false;
    }
    return Objects.equals(productId, that.productId)
        && Objects.equals(nutrientCode, that.nutrientCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productId, nutrientCode);
  }
}
