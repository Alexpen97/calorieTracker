package com.nutritrack.diary.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "diary_entry")
public class DiaryEntry {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "product_id")
  private UUID productId;

  @Column(name = "submission_id")
  private UUID submissionId;

  @Column(name = "product_name", nullable = false, columnDefinition = "TEXT")
  private String productName;

  @Column(columnDefinition = "TEXT")
  private String brand;

  @Column(name = "weight_g", nullable = false)
  private BigDecimal weightG;

  @Enumerated(EnumType.STRING)
  @Column(name = "meal_type", nullable = false, length = 16)
  private MealType mealType;

  @Column(name = "consumed_at", nullable = false)
  private Instant consumedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @OneToMany(
      mappedBy = "entry",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<DiaryEntryNutrient> nutrients = new ArrayList<>();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public UUID getSubmissionId() {
    return submissionId;
  }

  public void setSubmissionId(UUID submissionId) {
    this.submissionId = submissionId;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public BigDecimal getWeightG() {
    return weightG;
  }

  public void setWeightG(BigDecimal weightG) {
    this.weightG = weightG;
  }

  public MealType getMealType() {
    return mealType;
  }

  public void setMealType(MealType mealType) {
    this.mealType = mealType;
  }

  public Instant getConsumedAt() {
    return consumedAt;
  }

  public void setConsumedAt(Instant consumedAt) {
    this.consumedAt = consumedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public List<DiaryEntryNutrient> getNutrients() {
    return nutrients;
  }

  public void setNutrients(List<DiaryEntryNutrient> nutrients) {
    this.nutrients = nutrients;
  }

  public void replaceNutrients(List<DiaryEntryNutrient> next) {
    nutrients.clear();
    for (DiaryEntryNutrient nutrient : next) {
      nutrient.setEntry(this);
      nutrients.add(nutrient);
    }
  }
}
