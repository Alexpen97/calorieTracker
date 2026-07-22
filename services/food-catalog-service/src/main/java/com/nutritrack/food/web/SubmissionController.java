package com.nutritrack.food.web;

import com.nutritrack.food.domain.SubmissionStatus;
import com.nutritrack.food.service.SubmissionService;
import com.nutritrack.food.web.dto.CreateSubmissionRequest;
import com.nutritrack.food.web.dto.RejectSubmissionRequest;
import com.nutritrack.food.web.dto.SubmissionResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/submissions")
public class SubmissionController {

  private final SubmissionService submissionService;

  public SubmissionController(SubmissionService submissionService) {
    this.submissionService = submissionService;
  }

  @PostMapping
  public SubmissionResponse submit(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateSubmissionRequest request) {
    return submissionService.submit(UUID.fromString(jwt.getSubject()), request);
  }

  @GetMapping("/mine")
  public List<SubmissionResponse> mine(@AuthenticationPrincipal Jwt jwt) {
    return submissionService.listMine(UUID.fromString(jwt.getSubject()));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
  public List<SubmissionResponse> queue(
      @RequestParam(value = "status", defaultValue = "PENDING") SubmissionStatus status) {
    return submissionService.listByStatus(status);
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
  public SubmissionResponse approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    return submissionService.approve(id, UUID.fromString(jwt.getSubject()));
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
  public SubmissionResponse reject(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody(required = false) RejectSubmissionRequest request) {
    String note = request == null ? null : request.note();
    return submissionService.reject(id, UUID.fromString(jwt.getSubject()), note);
  }
}
