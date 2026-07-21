package com.nutritrack.diary.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, UUID> {
  @EntityGraph(attributePaths = "nutrients")
  List<DiaryEntry> findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtDesc(
      UUID userId, Instant from, Instant to);

  @EntityGraph(attributePaths = "nutrients")
  Optional<DiaryEntry> findByIdAndUserId(UUID id, UUID userId);
}
