package com.nutritrack.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "nutrient_reference_intake")
@IdClass(NutrientReferenceIntakeId.class)
public class NutrientReferenceIntake {

  @Id
  @Column(name = "nutrient_code", nullable = false, length = 64)
  private String nutrientCode;

  @Id
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Sex sex;

  @Id
  @Column(name = "age_min", nullable = false)
  private Short ageMin;

  @Column(name = "age_max", nullable = false)
  private Short ageMax;

  @Column(name = "daily_amount", nullable = false)
  private BigDecimal dailyAmount;

  @Column(nullable = false, length = 16)
  private String unit;

  @Column(nullable = false, length = 16)
  private String basis;

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

  public void setAgeMin(Integer ageMin) {
    this.ageMin = ageMin == null ? null : ageMin.shortValue();
  }

  public Short getAgeMax() {
    return ageMax;
  }

  public void setAgeMax(Integer ageMax) {
    this.ageMax = ageMax == null ? null : ageMax.shortValue();
  }

  public BigDecimal getDailyAmount() {
    return dailyAmount;
  }

  public void setDailyAmount(BigDecimal dailyAmount) {
    this.dailyAmount = dailyAmount;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public String getBasis() {
    return basis;
  }

  public void setBasis(String basis) {
    this.basis = basis;
  }
}
