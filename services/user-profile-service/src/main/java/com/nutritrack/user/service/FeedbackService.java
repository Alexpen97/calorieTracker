package com.nutritrack.user.service;

import com.nutritrack.user.domain.AppUser;
import com.nutritrack.user.domain.AppUserRepository;
import com.nutritrack.user.domain.FeedbackStatus;
import com.nutritrack.user.domain.UserFeedback;
import com.nutritrack.user.domain.UserFeedbackRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FeedbackService {

  private final AppUserRepository userRepository;
  private final UserFeedbackRepository feedbackRepository;

  public FeedbackService(
      AppUserRepository userRepository, UserFeedbackRepository feedbackRepository) {
    this.userRepository = userRepository;
    this.feedbackRepository = feedbackRepository;
  }

  @Transactional
  public UserFeedback create(UUID userId, String message, String appVersion) {
    AppUser user = requireUser(userId);
    Instant now = Instant.now();
    UserFeedback feedback = new UserFeedback();
    feedback.setId(UUID.randomUUID());
    feedback.setUser(user);
    feedback.setMessage(message.trim());
    feedback.setStatus(FeedbackStatus.PENDING);
    feedback.setAppVersion(blankToNull(appVersion));
    feedback.setCreatedAt(now);
    feedback.setUpdatedAt(now);
    return feedbackRepository.save(feedback);
  }

  @Transactional(readOnly = true)
  public List<UserFeedback> listMine(UUID userId) {
    requireUser(userId);
    return feedbackRepository.findByUser_IdOrderByCreatedAtDesc(userId);
  }

  @Transactional
  public UserFeedback updateStatus(UUID feedbackId, FeedbackStatus status) {
    UserFeedback feedback =
        feedbackRepository
            .findById(feedbackId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback not found"));
    feedback.setStatus(status);
    feedback.setUpdatedAt(Instant.now());
    return feedbackRepository.save(feedback);
  }

  private AppUser requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
