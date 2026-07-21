package com.nutritrack.diary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "diary_entry_nutrient")
@IdClass(DiaryEntryNutrientId.class)
public class DiaryEntryNutrient {

  @Id
  @Column(name = "entry_id")
  private UUID entryId;

  @Id
  @Column(name = "nutrient_code", length = 64)
  private String nutrientCode;

  @Column(name = "amount_per_100g", nullable = false)
  private BigDecimal amountPer100g;

  @Column(nullable = false, length = 16)
  private String unit;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "entry_id", insertable = false, updatable = false)
  private DiaryEntry entry;

  public UUID getEntryId() {
    return entryId;
  }

  public void setEntryId(UUID entryId) {
    this.entryId = entryId;
  }

  public String getNutrientCode() {
    return nutrientCode;
  }

  public void setNutrientCode(String nutrientCode) {
    this.nutrientCode = nutrientCode;
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

  public DiaryEntry getEntry() {
    return entry;
  }

  public void setEntry(DiaryEntry entry) {
    this.entry = entry;
    if (entry != null) {
      this.entryId = entry.getId();
    }
  }
}
