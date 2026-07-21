package com.nutritrack.user.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BodyWeightLogRepository extends JpaRepository<BodyWeightLog, UUID> {
  List<BodyWeightLog> findByUser_IdOrderByMeasuredAtDesc(UUID userId);

  List<BodyWeightLog> findByUser_IdAndMeasuredAtGreaterThanEqualOrderByMeasuredAtDesc(
      UUID userId, Instant from);

  List<BodyWeightLog> findByUser_IdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(
      UUID userId, Instant to);

  List<BodyWeightLog> findByUser_IdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
      UUID userId, Instant from, Instant to);
}
