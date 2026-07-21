package com.nutritrack.diary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "water_intake")
public class WaterIntake {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "amount_ml", nullable = false)
  private BigDecimal amountMl;

  @Column(name = "logged_at", nullable = false)
  private Instant loggedAt;

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

  public BigDecimal getAmountMl() {
    return amountMl;
  }

  public void setAmountMl(BigDecimal amountMl) {
    this.amountMl = amountMl;
  }

  public Instant getLoggedAt() {
    return loggedAt;
  }

  public void setLoggedAt(Instant loggedAt) {
    this.loggedAt = loggedAt;
  }
}
