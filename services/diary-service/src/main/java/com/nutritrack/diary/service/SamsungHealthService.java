package com.nutritrack.diary.service;

import com.nutritrack.diary.config.DiaryProperties;
import com.nutritrack.diary.domain.HealthActivityDaily;
import com.nutritrack.diary.domain.HealthActivityDailyRepository;
import com.nutritrack.diary.domain.HealthActivityProvider;
import com.nutritrack.diary.domain.HealthIntegrationConnection;
import com.nutritrack.diary.domain.HealthIntegrationConnectionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SamsungHealthService {

  public static final HealthActivityProvider PROVIDER = HealthActivityProvider.SAMSUNG_HEALTH;

  private final DiaryProperties properties;
  private final HealthActivityDailyRepository activityRepository;
  private final HealthIntegrationConnectionRepository connectionRepository;

  public SamsungHealthService(
      DiaryProperties properties,
      HealthActivityDailyRepository activityRepository,
      HealthIntegrationConnectionRepository connectionRepository) {
    this.properties = properties;
    this.activityRepository = activityRepository;
    this.connectionRepository = connectionRepository;
  }

  @Transactional(readOnly = true)
  public StatusResponse status(UUID userId) {
    if (!properties.samsungHealthEnabled()) {
      return new StatusResponse(false, false, "DISABLED", null, "Samsung Health integration is disabled");
    }
    Optional<HealthIntegrationConnection> connection =
        connectionRepository.findByUserIdAndProvider(userId, PROVIDER);
    if (connection.isEmpty()) {
      return new StatusResponse(true, false, "DISCONNECTED", null, null);
    }
    HealthIntegrationConnection row = connection.get();
    return new StatusResponse(
        true,
        row.isConnected(),
        row.getPermissionState(),
        row.getLastSyncedAt(),
        row.getLastError());
  }

  @Transactional
  public SyncResponse sync(UUID userId, SyncRequest request) {
    requireEnabled();
    if (request.zone() == null || request.zone().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zone is required");
    }
    if (request.days() == null || request.days().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must not be empty");
    }

    Instant now = Instant.now();
    String permissionState =
        request.permissionState() == null || request.permissionState().isBlank()
            ? "GRANTED"
            : request.permissionState().trim();
    List<SyncedDay> synced = new ArrayList<>();

    for (DayBurn day : request.days()) {
      if (day.localDate() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "localDate is required");
      }
      BigDecimal selected = resolveSelectedBurn(day);
      if (selected.compareTo(BigDecimal.ZERO) < 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "burned calories must be >= 0");
      }
      HealthActivityDaily activity =
          activityRepository
              .findByUserIdAndProviderAndLocalDateAndZoneId(
                  userId, PROVIDER, day.localDate(), request.zone())
              .orElseGet(HealthActivityDaily::new);
      if (activity.getId() == null) {
        activity.setId(UUID.randomUUID());
        activity.setUserId(userId);
        activity.setProvider(PROVIDER);
        activity.setLocalDate(day.localDate());
        activity.setZoneId(request.zone());
      }
      activity.setActiveEnergyKcal(day.activeEnergyKcal());
      activity.setTotalEnergyKcal(day.totalEnergyKcal());
      activity.setSelectedBurnKcal(selected);
      activity.setSourceRecordCount(Math.max(0, day.sourceRecordCount()));
      activity.setSyncedAt(now);
      activity.setPermissionState(permissionState);
      activityRepository.save(activity);
      synced.add(new SyncedDay(day.localDate(), selected));
    }

    HealthIntegrationConnection connection =
        connectionRepository
            .findByUserIdAndProvider(userId, PROVIDER)
            .orElseGet(HealthIntegrationConnection::new);
    connection.setUserId(userId);
    connection.setProvider(PROVIDER);
    connection.setConnected(true);
    connection.setPermissionState(permissionState);
    connection.setLastSyncedAt(now);
    connection.setLastError(null);
    connection.setUpdatedAt(now);
    connectionRepository.save(connection);

    return new SyncResponse(PROVIDER.name(), now, synced);
  }

  @Transactional
  public void disconnect(UUID userId) {
    requireEnabled();
    activityRepository.deleteByUserIdAndProvider(userId, PROVIDER);
    connectionRepository.deleteByUserIdAndProvider(userId, PROVIDER);
  }

  private void requireEnabled() {
    if (!properties.samsungHealthEnabled()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Samsung Health integration is disabled");
    }
  }

  private static BigDecimal resolveSelectedBurn(DayBurn day) {
    if (day.selectedBurnKcal() != null) {
      return day.selectedBurnKcal();
    }
    if (day.activeEnergyKcal() != null) {
      return day.activeEnergyKcal();
    }
    if (day.totalEnergyKcal() != null) {
      return day.totalEnergyKcal();
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "selectedBurnKcal, activeEnergyKcal, or totalEnergyKcal is required");
  }

  public record StatusResponse(
      boolean enabled,
      boolean connected,
      String permissionState,
      Instant lastSyncedAt,
      String lastError) {}

  public record SyncRequest(String zone, String permissionState, List<DayBurn> days) {}

  public record DayBurn(
      LocalDate localDate,
      BigDecimal activeEnergyKcal,
      BigDecimal totalEnergyKcal,
      BigDecimal selectedBurnKcal,
      int sourceRecordCount) {}

  public record SyncResponse(String provider, Instant syncedAt, List<SyncedDay> days) {}

  public record SyncedDay(LocalDate localDate, BigDecimal selectedBurnKcal) {}
}
