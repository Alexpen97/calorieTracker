package com.nutritrack.diary.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthActivityDailyRepository extends JpaRepository<HealthActivityDaily, UUID> {

  Optional<HealthActivityDaily> findByUserIdAndProviderAndLocalDateAndZoneId(
      UUID userId, HealthActivityProvider provider, LocalDate localDate, String zoneId);

  List<HealthActivityDaily> findByUserIdAndProviderAndLocalDateBetween(
      UUID userId, HealthActivityProvider provider, LocalDate from, LocalDate to);

  void deleteByUserIdAndProvider(UUID userId, HealthActivityProvider provider);
}
