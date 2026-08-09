package com.nutritrack.user.web;

import com.nutritrack.user.config.UserProperties;
import com.nutritrack.user.domain.UpdateMessage;
import com.nutritrack.user.service.UpdateMessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UpdateMessageController {

  private final UpdateMessageService updateMessageService;
  private final UserProperties properties;

  public UpdateMessageController(
      UpdateMessageService updateMessageService, UserProperties properties) {
    this.updateMessageService = updateMessageService;
    this.properties = properties;
  }

  @PostMapping("/api/users/internal/update-messages")
  public UpdateMessageResponse pushInternal(
      @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
      @Valid @RequestBody PushUpdateMessageRequest request) {
    requireInternalKey(apiKey);
    return UpdateMessageResponse.from(push(request));
  }

  @PostMapping("/api/users/admin/update-messages")
  public UpdateMessageResponse pushAdmin(@Valid @RequestBody PushUpdateMessageRequest request) {
    return UpdateMessageResponse.from(push(request));
  }

  @GetMapping("/api/users/me/update-messages/pending")
  public ResponseEntity<UpdateMessageResponse> pending(@AuthenticationPrincipal Jwt jwt) {
    return updateMessageService
        .findPendingForUser(UUID.fromString(jwt.getSubject()))
        .map(UpdateMessageResponse::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PostMapping("/api/users/me/update-messages/{id}/acknowledge")
  public ResponseEntity<Void> acknowledge(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
    updateMessageService.acknowledge(UUID.fromString(jwt.getSubject()), id);
    return ResponseEntity.noContent().build();
  }

  private UpdateMessage push(PushUpdateMessageRequest request) {
    return updateMessageService.push(
        request.title(),
        request.body(),
        request.imageUrl(),
        request.actionLabel(),
        request.actionUrl());
  }

  private void requireInternalKey(String apiKey) {
    if (apiKey == null || !apiKey.equals(properties.internalApiKey())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal API key");
    }
  }

  public record PushUpdateMessageRequest(
      @NotBlank String title,
      @NotBlank String body,
      String imageUrl,
      String actionLabel,
      String actionUrl) {}

  public record UpdateMessageResponse(
      UUID id,
      String title,
      String body,
      String imageUrl,
      String actionLabel,
      String actionUrl,
      Instant pushedAt) {
    static UpdateMessageResponse from(UpdateMessage message) {
      return new UpdateMessageResponse(
          message.getId(),
          message.getTitle(),
          message.getBody(),
          message.getImageUrl(),
          message.getActionLabel(),
          message.getActionUrl(),
          message.getPushedAt());
    }
  }
}
