package com.nutritrack.nevo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "nevo_food")
public class NevoFood {

  @Id
  @Column(name = "nevo_code", length = 32)
  private String nevoCode;

  @Column(name = "food_name_en", nullable = false)
  private String foodNameEn;

  @Column(name = "food_name_nl")
  private String foodNameNl;

  @Column(name = "food_group")
  private String foodGroup;

  private String synonym;

  @Column(name = "quantity_label")
  private String quantityLabel;

  private String remark;

  @Column(name = "nevo_version", nullable = false)
  private String nevoVersion;

  @Column(name = "search_document", nullable = false)
  private String searchDocument;

  @Column(name = "energy_kcal")
  private BigDecimal energyKcal;

  @Column(name = "protein_g")
  private BigDecimal proteinG;

  @Column(name = "fat_g")
  private BigDecimal fatG;

  @Column(name = "carbohydrate_g")
  private BigDecimal carbohydrateG;

  @Column(name = "sugars_g")
  private BigDecimal sugarsG;

  @Column(name = "fiber_g")
  private BigDecimal fiberG;

  @Column(name = "sodium_mg")
  private BigDecimal sodiumMg;

  public String getNevoCode() {
    return nevoCode;
  }

  public void setNevoCode(String nevoCode) {
    this.nevoCode = nevoCode;
  }

  public String getFoodNameEn() {
    return foodNameEn;
  }

  public void setFoodNameEn(String foodNameEn) {
    this.foodNameEn = foodNameEn;
  }

  public String getFoodNameNl() {
    return foodNameNl;
  }

  public void setFoodNameNl(String foodNameNl) {
    this.foodNameNl = foodNameNl;
  }

  public String getFoodGroup() {
    return foodGroup;
  }

  public void setFoodGroup(String foodGroup) {
    this.foodGroup = foodGroup;
  }

  public String getSynonym() {
    return synonym;
  }

  public void setSynonym(String synonym) {
    this.synonym = synonym;
  }

  public String getQuantityLabel() {
    return quantityLabel;
  }

  public void setQuantityLabel(String quantityLabel) {
    this.quantityLabel = quantityLabel;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public String getNevoVersion() {
    return nevoVersion;
  }

  public void setNevoVersion(String nevoVersion) {
    this.nevoVersion = nevoVersion;
  }

  public String getSearchDocument() {
    return searchDocument;
  }

  public void setSearchDocument(String searchDocument) {
    this.searchDocument = searchDocument;
  }

  public BigDecimal getEnergyKcal() {
    return energyKcal;
  }

  public void setEnergyKcal(BigDecimal energyKcal) {
    this.energyKcal = energyKcal;
  }

  public BigDecimal getProteinG() {
    return proteinG;
  }

  public void setProteinG(BigDecimal proteinG) {
    this.proteinG = proteinG;
  }

  public BigDecimal getFatG() {
    return fatG;
  }

  public void setFatG(BigDecimal fatG) {
    this.fatG = fatG;
  }

  public BigDecimal getCarbohydrateG() {
    return carbohydrateG;
  }

  public void setCarbohydrateG(BigDecimal carbohydrateG) {
    this.carbohydrateG = carbohydrateG;
  }

  public BigDecimal getSugarsG() {
    return sugarsG;
  }

  public void setSugarsG(BigDecimal sugarsG) {
    this.sugarsG = sugarsG;
  }

  public BigDecimal getFiberG() {
    return fiberG;
  }

  public void setFiberG(BigDecimal fiberG) {
    this.fiberG = fiberG;
  }

  public BigDecimal getSodiumMg() {
    return sodiumMg;
  }

  public void setSodiumMg(BigDecimal sodiumMg) {
    this.sodiumMg = sodiumMg;
  }
}
