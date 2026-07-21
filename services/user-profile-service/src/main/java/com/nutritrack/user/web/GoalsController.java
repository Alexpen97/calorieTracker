package com.nutritrack.user.web;

import com.nutritrack.user.service.GoalsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoalsController {

  private final GoalsService goalsService;

  public GoalsController(GoalsService goalsService) {
    this.goalsService = goalsService;
  }

  @GetMapping("/api/users/me/goals")
  public List<GoalsService.GoalResponse> list(@AuthenticationPrincipal Jwt jwt) {
    return goalsService.list(UUID.fromString(jwt.getSubject()));
  }

  @PutMapping("/api/users/me/goals")
  public List<GoalsService.GoalResponse> override(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GoalsRequest request) {
    return goalsService.override(
        UUID.fromString(jwt.getSubject()),
        request.goals().stream()
            .map(
                goal ->
                    new GoalsService.GoalOverride(
                        goal.nutrientCode(), goal.dailyTarget(), goal.unit()))
            .toList());
  }

  @PostMapping("/api/users/me/goals/recalculate")
  public GoalsService.RecalculateResponse recalculate(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(name = "apply", defaultValue = "false") boolean apply) {
    return goalsService.recalculate(UUID.fromString(jwt.getSubject()), apply);
  }

  public record GoalsRequest(@NotEmpty List<@Valid GoalRequest> goals) {}

  public record GoalRequest(
      @NotBlank String nutrientCode,
      @NotNull @Positive BigDecimal dailyTarget,
      @NotBlank String unit) {}
}
