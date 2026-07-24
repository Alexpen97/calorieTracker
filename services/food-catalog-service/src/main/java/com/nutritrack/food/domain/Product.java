package com.nutritrack.food.domain;

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
@Table(name = "product")
public class Product {

  @Id private UUID id;

  @Column(length = 32, unique = true)
  private String barcode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ProductSource source;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String name;

  @Column(name = "generic_name", columnDefinition = "TEXT")
  private String genericName;

  @Column(columnDefinition = "TEXT")
  private String brand;

  @Column(name = "quantity_label", columnDefinition = "TEXT")
  private String quantityLabel;

  @Column(name = "serving_size_g")
  private BigDecimal servingSizeG;

  @Column(name = "image_url", columnDefinition = "TEXT")
  private String imageUrl;

  @Column(name = "nutri_score", length = 1)
  private String nutriScore;

  @Column(name = "ingredients_text", columnDefinition = "TEXT")
  private String ingredientsText;

  @Column(name = "allergen_tags", columnDefinition = "TEXT")
  private String allergenTags;

  @Column(name = "off_last_synced_at")
  private Instant offLastSyncedAt;

  @Column(name = "search_document", columnDefinition = "TEXT")
  private String searchDocument;

  @OneToMany(
      mappedBy = "product",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<ProductNutrient> nutrients = new ArrayList<>();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public ProductSource getSource() {
    return source;
  }

  public void setSource(ProductSource source) {
    this.source = source;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGenericName() {
    return genericName;
  }

  public void setGenericName(String genericName) {
    this.genericName = genericName;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getQuantityLabel() {
    return quantityLabel;
  }

  public void setQuantityLabel(String quantityLabel) {
    this.quantityLabel = quantityLabel;
  }

  public BigDecimal getServingSizeG() {
    return servingSizeG;
  }

  public void setServingSizeG(BigDecimal servingSizeG) {
    this.servingSizeG = servingSizeG;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getNutriScore() {
    return nutriScore;
  }

  public void setNutriScore(String nutriScore) {
    this.nutriScore = nutriScore;
  }

  public String getIngredientsText() {
    return ingredientsText;
  }

  public void setIngredientsText(String ingredientsText) {
    this.ingredientsText = ingredientsText;
  }

  public String getAllergenTags() {
    return allergenTags;
  }

  public void setAllergenTags(String allergenTags) {
    this.allergenTags = allergenTags;
  }

  public Instant getOffLastSyncedAt() {
    return offLastSyncedAt;
  }

  public void setOffLastSyncedAt(Instant offLastSyncedAt) {
    this.offLastSyncedAt = offLastSyncedAt;
  }

  public String getSearchDocument() {
    return searchDocument;
  }

  public void setSearchDocument(String searchDocument) {
    this.searchDocument = searchDocument;
  }

  public void refreshSearchDocument() {
    String namePart = name == null ? "" : name;
    String genericPart = genericName == null ? "" : genericName;
    String brandPart = brand == null ? "" : brand;
    this.searchDocument = (namePart + " " + genericPart + " " + brandPart).trim().toLowerCase();
  }

  public List<ProductNutrient> getNutrients() {
    return nutrients;
  }

  public void setNutrients(List<ProductNutrient> nutrients) {
    this.nutrients = nutrients;
  }

  public void replaceNutrients(List<ProductNutrient> next) {
    nutrients.clear();
    for (ProductNutrient nutrient : next) {
      nutrient.setProduct(this);
      nutrients.add(nutrient);
    }
  }
}
