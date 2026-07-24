package com.nutritrack.food.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_nutrient")
@IdClass(ProductNutrientId.class)
public class ProductNutrient {

  @Id
  @Column(name = "product_id")
  private UUID productId;

  @Id
  @Column(name = "nutrient_code", length = 64)
  private String nutrientCode;

  @Column(name = "amount_per_100g", nullable = false)
  private BigDecimal amountPer100g;

  @Column(nullable = false, length = 16)
  private String unit;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private NutrientSource source = NutrientSource.OFF;

  @Column(name = "source_ref", length = 64)
  private String sourceRef;

  @Column(length = 16)
  private String confidence;

  @Column(nullable = false)
  private boolean estimated;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", insertable = false, updatable = false)
  private Product product;

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

  public NutrientSource getSource() {
    return source;
  }

  public void setSource(NutrientSource source) {
    this.source = source;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public void setSourceRef(String sourceRef) {
    this.sourceRef = sourceRef;
  }

  public String getConfidence() {
    return confidence;
  }

  public void setConfidence(String confidence) {
    this.confidence = confidence;
  }

  public boolean isEstimated() {
    return estimated;
  }

  public void setEstimated(boolean estimated) {
    this.estimated = estimated;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
    if (product != null) {
      this.productId = product.getId();
    }
  }
}
