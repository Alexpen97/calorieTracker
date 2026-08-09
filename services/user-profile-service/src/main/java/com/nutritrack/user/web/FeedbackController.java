package com.nutritrack.user.web;

import com.nutritrack.user.domain.FeedbackStatus;
import com.nutritrack.user.domain.UserFeedback;
import com.nutritrack.user.service.FeedbackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeedbackController {

  private final FeedbackService feedbackService;

  public FeedbackController(FeedbackService feedbackService) {
    this.feedbackService = feedbackService;
  }

  @PostMapping("/api/users/me/feedback")
  public FeedbackResponse create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateFeedbackRequest request) {
    UserFeedback feedback =
        feedbackService.create(
            UUID.fromString(jwt.getSubject()), request.message(), request.appVersion());
    return FeedbackResponse.from(feedback);
  }

  @GetMapping("/api/users/me/feedback")
  public List<FeedbackResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
    return feedbackService.listMine(UUID.fromString(jwt.getSubject())).stream()
        .map(FeedbackResponse::from)
        .toList();
  }

  @PatchMapping("/api/users/feedback/{id}/status")
  public FeedbackResponse updateStatus(
      @PathVariable("id") UUID id, @Valid @RequestBody UpdateFeedbackStatusRequest request) {
    UserFeedback feedback = feedbackService.updateStatus(id, request.status());
    return FeedbackResponse.from(feedback);
  }

  public record CreateFeedbackRequest(
      @NotBlank @Size(min = 10, max = 2000) String message,
      @Size(max = 64) String appVersion) {}

  public record UpdateFeedbackStatusRequest(@NotNull FeedbackStatus status) {}

  public record FeedbackResponse(
      UUID id,
      String message,
      FeedbackStatus status,
      String appVersion,
      Instant createdAt,
      Instant updatedAt) {
    static FeedbackResponse from(UserFeedback feedback) {
      return new FeedbackResponse(
          feedback.getId(),
          feedback.getMessage(),
          feedback.getStatus(),
          feedback.getAppVersion(),
          feedback.getCreatedAt(),
          feedback.getUpdatedAt());
    }
  }
}
