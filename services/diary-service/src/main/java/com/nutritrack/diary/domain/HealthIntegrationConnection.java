package com.nutritrack.diary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "health_integration_connection")
@IdClass(HealthIntegrationConnection.Pk.class)
public class HealthIntegrationConnection {

  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 32)
  private HealthActivityProvider provider;

  @Column(name = "connected", nullable = false)
  private boolean connected;

  @Column(name = "permission_state", nullable = false, length = 32)
  private String permissionState;

  @Column(name = "last_synced_at")
  private Instant lastSyncedAt;

  @Column(name = "last_error")
  private String lastError;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

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

  public boolean isConnected() {
    return connected;
  }

  public void setConnected(boolean connected) {
    this.connected = connected;
  }

  public String getPermissionState() {
    return permissionState;
  }

  public void setPermissionState(String permissionState) {
    this.permissionState = permissionState;
  }

  public Instant getLastSyncedAt() {
    return lastSyncedAt;
  }

  public void setLastSyncedAt(Instant lastSyncedAt) {
    this.lastSyncedAt = lastSyncedAt;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public static class Pk implements Serializable {
    private UUID userId;
    private HealthActivityProvider provider;

    public Pk() {}

    public Pk(UUID userId, HealthActivityProvider provider) {
      this.userId = userId;
      this.provider = provider;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof Pk pk)) {
        return false;
      }
      return Objects.equals(userId, pk.userId) && provider == pk.provider;
    }

    @Override
    public int hashCode() {
      return Objects.hash(userId, provider);
    }
  }
}
