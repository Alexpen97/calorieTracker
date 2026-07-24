package com.nutritrack.nevo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "nevo_nutrient_value")
public class NevoNutrientValue {

  @Id private UUID id;

  @Column(name = "nevo_code", nullable = false, length = 32)
  private String nevoCode;

  @Column(name = "nutrient_code", length = 64)
  private String nutrientCode;

  @Column(name = "nevo_column", nullable = false)
  private String nevoColumn;

  @Column(name = "amount_per_100g")
  private BigDecimal amountPer100g;

  @Column(length = 16)
  private String unit;

  @Column(name = "raw_value")
  private String rawValue;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getNevoCode() {
    return nevoCode;
  }

  public void setNevoCode(String nevoCode) {
    this.nevoCode = nevoCode;
  }

  public String getNutrientCode() {
    return nutrientCode;
  }

  public void setNutrientCode(String nutrientCode) {
    this.nutrientCode = nutrientCode;
  }

  public String getNevoColumn() {
    return nevoColumn;
  }

  public void setNevoColumn(String nevoColumn) {
    this.nevoColumn = nevoColumn;
  }

  public BigDecimal getAmountPer100g() {
    return amountPer100g;
  }

  public void setAmountPer100g(BigDecimal amountPer100g) {
    this.amountPer100g = amountPer100g;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public String getRawValue() {
    return rawValue;
  }

  public void setRawValue(String rawValue) {
    this.rawValue = rawValue;
  }
}
