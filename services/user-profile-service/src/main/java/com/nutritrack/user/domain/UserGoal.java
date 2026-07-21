package com.nutritrack.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_goal")
public class UserGoal {

  @EmbeddedId private UserGoalId id = new UserGoalId();

  @MapsId("userId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(name = "daily_target", nullable = false)
  private BigDecimal dailyTarget;

  @Column(nullable = false, length = 16)
  private String unit;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private GoalOrigin origin;

  @Column(name = "computed_at")
  private Instant computedAt;

  public UserGoalId getId() {
    return id;
  }

  public void setId(UserGoalId id) {
    this.id = id;
  }

  public AppUser getUser() {
    return user;
  }

  public void setUser(AppUser user) {
    this.user = user;
    this.id.setUserId(user == null ? null : user.getId());
  }

  public String getNutrientCode() {
    return id.getNutrientCode();
  }

  public void setNutrientCode(String nutrientCode) {
    this.id.setNutrientCode(nutrientCode);
  }

  public BigDecimal getDailyTarget() {
    return dailyTarget;
  }

  public void setDailyTarget(BigDecimal dailyTarget) {
    this.dailyTarget = dailyTarget;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public GoalOrigin getOrigin() {
    return origin;
  }

  public void setOrigin(GoalOrigin origin) {
    this.origin = origin;
  }

  public Instant getComputedAt() {
    return computedAt;
  }

  public void setComputedAt(Instant computedAt) {
    this.computedAt = computedAt;
  }
}
