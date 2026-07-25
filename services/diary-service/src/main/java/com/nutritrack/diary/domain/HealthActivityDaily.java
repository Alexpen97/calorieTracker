package com.nutritrack.diary.domain;

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
@Table(name = "health_activity_daily")
public class HealthActivityDaily {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 32)
  private HealthActivityProvider provider;

  @Column(name = "local_date", nullable = false)
  private LocalDate localDate;

  @Column(name = "zone_id", nullable = false, length = 64)
  private String zoneId;

  @Column(name = "active_energy_kcal")
  private BigDecimal activeEnergyKcal;

  @Column(name = "total_energy_kcal")
  private BigDecimal totalEnergyKcal;

  @Column(name = "selected_burn_kcal", nullable = false)
  private BigDecimal selectedBurnKcal;

  @Column(name = "source_record_count", nullable = false)
  private int sourceRecordCount;

  @Column(name = "synced_at", nullable = false)
  private Instant syncedAt;

  @Column(name = "permission_state", nullable = false, length = 32)
  private String permissionState;

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

  public HealthActivityProvider getProvider() {
    return provider;
  }

  public void setProvider(HealthActivityProvider provider) {
    this.provider = provider;
  }

  public LocalDate getLocalDate() {
    return localDate;
  }

  public void setLocalDate(LocalDate localDate) {
    this.localDate = localDate;
  }

  public String getZoneId() {
    return zoneId;
  }

  public void setZoneId(String zoneId) {
    this.zoneId = zoneId;
  }

  public BigDecimal getActiveEnergyKcal() {
    return activeEnergyKcal;
  }

  public void setActiveEnergyKcal(BigDecimal activeEnergyKcal) {
    this.activeEnergyKcal = activeEnergyKcal;
  }

  public BigDecimal getTotalEnergyKcal() {
    return totalEnergyKcal;
  }

  public void setTotalEnergyKcal(BigDecimal totalEnergyKcal) {
    this.totalEnergyKcal = totalEnergyKcal;
  }

  public BigDecimal getSelectedBurnKcal() {
    return selectedBurnKcal;
  }

  public void setSelectedBurnKcal(BigDecimal selectedBurnKcal) {
    this.selectedBurnKcal = selectedBurnKcal;
  }

  public int getSourceRecordCount() {
    return sourceRecordCount;
  }

  public void setSourceRecordCount(int sourceRecordCount) {
    this.sourceRecordCount = sourceRecordCount;
  }

  public Instant getSyncedAt() {
    return syncedAt;
  }

  public void setSyncedAt(Instant syncedAt) {
    this.syncedAt = syncedAt;
  }

  public String getPermissionState() {
    return permissionState;
  }

  public void setPermissionState(String permissionState) {
    this.permissionState = permissionState;
  }
}
