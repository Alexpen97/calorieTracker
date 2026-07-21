package com.nutritrack.food.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "nutrient")
public class Nutrient {

  @Id
  @Column(length = 64)
  private String code;

  @Column(name = "display_name", nullable = false, columnDefinition = "TEXT")
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private NutrientCategory category;

  @Column(name = "default_unit", nullable = false, length = 16)
  private String defaultUnit;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "body_effects", columnDefinition = "TEXT")
  private String bodyEffects;

  @Column(name = "deficiency_effects", columnDefinition = "TEXT")
  private String deficiencyEffects;

  @Column(name = "excess_effects", columnDefinition = "TEXT")
  private String excessEffects;

  @Column(name = "common_sources", columnDefinition = "TEXT")
  private String commonSources;

  @Column(name = "content_source", columnDefinition = "TEXT")
  private String contentSource;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public NutrientCategory getCategory() {
    return category;
  }

  public void setCategory(NutrientCategory category) {
    this.category = category;
  }

  public String getDefaultUnit() {
    return defaultUnit;
  }

  public void setDefaultUnit(String defaultUnit) {
    this.defaultUnit = defaultUnit;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getBodyEffects() {
    return bodyEffects;
  }

  public void setBodyEffects(String bodyEffects) {
    this.bodyEffects = bodyEffects;
  }

  public String getDeficiencyEffects() {
    return deficiencyEffects;
  }

  public void setDeficiencyEffects(String deficiencyEffects) {
    this.deficiencyEffects = deficiencyEffects;
  }

  public String getExcessEffects() {
    return excessEffects;
  }

  public void setExcessEffects(String excessEffects) {
    this.excessEffects = excessEffects;
  }

  public String getCommonSources() {
    return commonSources;
  }

  public void setCommonSources(String commonSources) {
    this.commonSources = commonSources;
  }

  public String getContentSource() {
    return contentSource;
  }

  public void setContentSource(String contentSource) {
    this.contentSource = contentSource;
  }
}
