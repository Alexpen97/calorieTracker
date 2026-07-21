package com.nutritrack.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {

  @Id private UUID id;

  @Column(name = "google_sub", nullable = false, unique = true, length = 64)
  private String googleSub;

  @Column(nullable = false)
  private String email;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private UserRole role = UserRole.USER;

  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private Sex sex;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(name = "height_cm")
  private BigDecimal heightCm;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_level", length = 32)
  private ActivityLevel activityLevel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Objective objective = Objective.MAINTAIN;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getGoogleSub() {
    return googleSub;
  }

  public void setGoogleSub(String googleSub) {
    this.googleSub = googleSub;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  public Sex getSex() {
    return sex;
  }

  public void setSex(Sex sex) {
    this.sex = sex;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(LocalDate birthDate) {
    this.birthDate = birthDate;
  }

  public BigDecimal getHeightCm() {
    return heightCm;
  }

  public void setHeightCm(BigDecimal heightCm) {
    this.heightCm = heightCm;
  }

  public ActivityLevel getActivityLevel() {
    return activityLevel;
  }

  public void setActivityLevel(ActivityLevel activityLevel) {
    this.activityLevel = activityLevel;
  }

  public Objective getObjective() {
    return objective;
  }

  public void setObjective(Objective objective) {
    this.objective = objective;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
