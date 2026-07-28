package com.nutritrack.diary.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthIntegrationConnectionRepository
    extends JpaRepository<HealthIntegrationConnection, HealthIntegrationConnection.Pk> {

  Optional<HealthIntegrationConnection> findByUserIdAndProvider(
      UUID userId, HealthActivityProvider provider);

  void deleteByUserIdAndProvider(UUID userId, HealthActivityProvider provider);
}
