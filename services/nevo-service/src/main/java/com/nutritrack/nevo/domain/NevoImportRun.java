package com.nutritrack.nevo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nevo_import_run")
public class NevoImportRun {

  @Id private UUID id;

  @Column(name = "csv_filename", nullable = false)
  private String csvFilename;

  @Column(name = "nevo_version", nullable = false)
  private String nevoVersion;

  @Column(name = "food_count", nullable = false)
  private int foodCount;

  @Column(name = "nutrient_count", nullable = false)
  private int nutrientCount;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(name = "error_message")
  private String errorMessage;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getCsvFilename() {
    return csvFilename;
  }

  public void setCsvFilename(String csvFilename) {
    this.csvFilename = csvFilename;
  }

  public String getNevoVersion() {
    return nevoVersion;
  }

  public void setNevoVersion(String nevoVersion) {
    this.nevoVersion = nevoVersion;
  }

  public int getFoodCount() {
    return foodCount;
  }

  public void setFoodCount(int foodCount) {
    this.foodCount = foodCount;
  }

  public int getNutrientCount() {
    return nutrientCount;
  }

  public void setNutrientCount(int nutrientCount) {
    this.nutrientCount = nutrientCount;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(Instant finishedAt) {
    this.finishedAt = finishedAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
