package com.nutritrack.food.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_submission")
public class ProductSubmission {

  @Id private UUID id;

  @Column(name = "submitter_user_id", nullable = false)
  private UUID submitterUserId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private SubmissionStatus status;

  @Column(length = 32)
  private String barcode;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String name;

  @Column(columnDefinition = "TEXT")
  private String brand;

  @Column(name = "serving_size_g")
  private BigDecimal servingSizeG;

  /** JSON object: { "code": { "amountPer100g": number, "unit": "g" }, ... } */
  @Column(nullable = false, columnDefinition = "TEXT")
  private String nutrients;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "review_note", columnDefinition = "TEXT")
  private String reviewNote;

  @Column(name = "published_product_id")
  private UUID publishedProductId;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getSubmitterUserId() {
    return submitterUserId;
  }

  public void setSubmitterUserId(UUID submitterUserId) {
    this.submitterUserId = submitterUserId;
  }

  public SubmissionStatus getStatus() {
    return status;
  }

  public void setStatus(SubmissionStatus status) {
    this.status = status;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public BigDecimal getServingSizeG() {
    return servingSizeG;
  }

  public void setServingSizeG(BigDecimal servingSizeG) {
    this.servingSizeG = servingSizeG;
  }

  public String getNutrients() {
    return nutrients;
  }

  public void setNutrients(String nutrients) {
    this.nutrients = nutrients;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(Instant submittedAt) {
    this.submittedAt = submittedAt;
  }

  public UUID getReviewedBy() {
    return reviewedBy;
  }

  public void setReviewedBy(UUID reviewedBy) {
    this.reviewedBy = reviewedBy;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(Instant reviewedAt) {
    this.reviewedAt = reviewedAt;
  }

  public String getReviewNote() {
    return reviewNote;
  }

  public void setReviewNote(String reviewNote) {
    this.reviewNote = reviewNote;
  }

  public UUID getPublishedProductId() {
    return publishedProductId;
  }

  public void setPublishedProductId(UUID publishedProductId) {
    this.publishedProductId = publishedProductId;
  }
}
