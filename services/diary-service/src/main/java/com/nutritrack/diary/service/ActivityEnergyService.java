package com.nutritrack.diary.service;

import com.nutritrack.diary.domain.HealthActivityDaily;
import com.nutritrack.diary.domain.HealthActivityDailyRepository;
import com.nutritrack.diary.domain.HealthActivityProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityEnergyService {

  private final HealthActivityDailyRepository activityRepository;

  public ActivityEnergyService(HealthActivityDailyRepository activityRepository) {
    this.activityRepository = activityRepository;
  }

  @Transactional(readOnly = true)
  public Optional<BurnedEnergy> findBurnedEnergy(
      UUID userId, LocalDate date, String zoneId, HealthActivityProvider provider) {
    return activityRepository
        .findByUserIdAndProviderAndLocalDateAndZoneId(userId, provider, date, zoneId)
        .or(() -> findAnyZoneForDate(userId, provider, date))
        .map(
            activity ->
                new BurnedEnergy(
                    activity.getProvider().name(),
                    activity.getSelectedBurnKcal(),
                    activity.getSyncedAt()));
  }

  @Transactional(readOnly = true)
  public Map<LocalDate, BurnedEnergy> findBurnedEnergyRange(
      UUID userId, LocalDate from, LocalDate to, HealthActivityProvider provider) {
    List<HealthActivityDaily> rows =
        activityRepository.findByUserIdAndProviderAndLocalDateBetween(userId, provider, from, to);
    Map<LocalDate, BurnedEnergy> byDate = new HashMap<>();
    for (HealthActivityDaily activity : rows) {
      BurnedEnergy next =
          new BurnedEnergy(
              activity.getProvider().name(),
              activity.getSelectedBurnKcal(),
              activity.getSyncedAt());
      BurnedEnergy existing = byDate.get(activity.getLocalDate());
      if (existing == null || activity.getSyncedAt().isAfter(existing.syncedAt())) {
        byDate.put(activity.getLocalDate(), next);
      }
    }
    return byDate;
  }

  private Optional<HealthActivityDaily> findAnyZoneForDate(
      UUID userId, HealthActivityProvider provider, LocalDate date) {
    return activityRepository
        .findByUserIdAndProviderAndLocalDateBetween(userId, provider, date, date)
        .stream()
        .max((left, right) -> left.getSyncedAt().compareTo(right.getSyncedAt()));
  }

  public record BurnedEnergy(String provider, BigDecimal burnedCalories, Instant syncedAt) {}
}
