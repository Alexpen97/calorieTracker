package com.nutritrack.user.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, UUID> {
  List<UserFeedback> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}
