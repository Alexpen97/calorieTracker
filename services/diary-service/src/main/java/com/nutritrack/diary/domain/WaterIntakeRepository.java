package com.nutritrack.diary.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterIntakeRepository extends JpaRepository<WaterIntake, UUID> {
  List<WaterIntake> findByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtDesc(
      UUID userId, Instant from, Instant to);

  Optional<WaterIntake> findByIdAndUserId(UUID id, UUID userId);
}
