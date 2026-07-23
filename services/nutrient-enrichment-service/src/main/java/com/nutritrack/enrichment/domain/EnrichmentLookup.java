package com.nutritrack.enrichment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "enrichment_lookup")
public class EnrichmentLookup {

  @Id
  @Column(length = 32)
  private String barcode;

  @Enumerated(EnumType.STRING)
  @Column(name = "match_type", nullable = false, length = 16)
  private MatchType matchType;

  @Column(name = "fdc_id")
  private Long fdcId;

  @Column(name = "matched_description", columnDefinition = "TEXT")
  private String matchedDescription;

  private BigDecimal confidence;

  @Column(name = "nutrients_json", nullable = false, columnDefinition = "TEXT")
  private String nutrientsJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public MatchType getMatchType() {
    return matchType;
  }

  public void setMatchType(MatchType matchType) {
    this.matchType = matchType;
  }

  public Long getFdcId() {
    return fdcId;
  }

  public void setFdcId(Long fdcId) {
    this.fdcId = fdcId;
  }

  public String getMatchedDescription() {
    return matchedDescription;
  }

  public void setMatchedDescription(String matchedDescription) {
    this.matchedDescription = matchedDescription;
  }

  public BigDecimal getConfidence() {
    return confidence;
  }

  public void setConfidence(BigDecimal confidence) {
    this.confidence = confidence;
  }

  public String getNutrientsJson() {
    return nutrientsJson;
  }

  public void setNutrientsJson(String nutrientsJson) {
    this.nutrientsJson = nutrientsJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
