package com.nutritrack.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserGoalId implements Serializable {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "nutrient_code", nullable = false, length = 64)
  private String nutrientCode;

  public UserGoalId() {}

  public UserGoalId(UUID userId, String nutrientCode) {
    this.userId = userId;
    this.nutrientCode = nutrientCode;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
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
    if (!(o instanceof UserGoalId that)) {
      return false;
    }
    return Objects.equals(userId, that.userId)
        && Objects.equals(nutrientCode, that.nutrientCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, nutrientCode);
  }
}
