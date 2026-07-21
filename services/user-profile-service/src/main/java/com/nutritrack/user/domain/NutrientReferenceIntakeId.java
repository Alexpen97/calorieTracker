package com.nutritrack.user.domain;

import java.io.Serializable;
import java.util.Objects;

public class NutrientReferenceIntakeId implements Serializable {

  private String nutrientCode;
  private Sex sex;
  private Short ageMin;

  public NutrientReferenceIntakeId() {}

  public NutrientReferenceIntakeId(String nutrientCode, Sex sex, Short ageMin) {
    this.nutrientCode = nutrientCode;
    this.sex = sex;
    this.ageMin = ageMin;
  }

  public String getNutrientCode() {
    return nutrientCode;
  }

  public void setNutrientCode(String nutrientCode) {
    this.nutrientCode = nutrientCode;
  }

  public Sex getSex() {
    return sex;
  }

  public void setSex(Sex sex) {
    this.sex = sex;
  }

  public Short getAgeMin() {
    return ageMin;
  }

  public void setAgeMin(Short ageMin) {
    this.ageMin = ageMin;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NutrientReferenceIntakeId that)) {
      return false;
    }
    return Objects.equals(nutrientCode, that.nutrientCode)
        && sex == that.sex
        && Objects.equals(ageMin, that.ageMin);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nutrientCode, sex, ageMin);
  }
}
