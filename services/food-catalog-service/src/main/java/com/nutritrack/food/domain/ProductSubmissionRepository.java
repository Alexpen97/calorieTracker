package com.nutritrack.food.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductSubmissionRepository extends JpaRepository<ProductSubmission, UUID> {

  List<ProductSubmission> findBySubmitterUserIdOrderBySubmittedAtDesc(UUID submitterUserId);

  List<ProductSubmission> findByStatusOrderBySubmittedAtAsc(SubmissionStatus status);

  Optional<ProductSubmission> findByIdAndSubmitterUserId(UUID id, UUID submitterUserId);

  Optional<ProductSubmission> findFirstByBarcodeAndSubmitterUserIdAndStatusIn(
      String barcode, UUID submitterUserId, List<SubmissionStatus> statuses);

  @Query(
      """
      SELECT s FROM ProductSubmission s
      WHERE s.submitterUserId = :userId
        AND s.status IN :statuses
        AND (
          LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(s.brand, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        )
      ORDER BY s.submittedAt DESC
      """)
  List<ProductSubmission> searchOwn(
      @Param("userId") UUID userId,
      @Param("q") String q,
      @Param("statuses") List<SubmissionStatus> statuses,
      Pageable pageable);

  @Query(
      """
      SELECT s FROM ProductSubmission s
      WHERE s.status = com.nutritrack.food.domain.SubmissionStatus.PENDING
        AND (
          LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(s.brand, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        )
      """)
  List<ProductSubmission> findPendingNameMatches(@Param("q") String q, Pageable pageable);
}
