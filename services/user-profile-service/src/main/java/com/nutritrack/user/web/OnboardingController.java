package com.nutritrack.user.web;

import com.nutritrack.user.domain.ActivityLevel;
import com.nutritrack.user.domain.Objective;
import com.nutritrack.user.domain.Sex;
import com.nutritrack.user.service.GoalsService;
import com.nutritrack.user.service.OnboardingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OnboardingController {

  private final OnboardingService onboardingService;

  public OnboardingController(OnboardingService onboardingService) {
    this.onboardingService = onboardingService;
  }

  @PostMapping("/api/users/me/onboarding")
  public OnboardingResponse complete(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OnboardingRequest request) {
    OnboardingService.OnboardingResult result =
        onboardingService.complete(
            UUID.fromString(jwt.getSubject()),
            new OnboardingService.OnboardingInput(
                request.sex(),
                request.birthDate(),
                request.heightCm(),
                request.weightKg(),
                request.activityLevel(),
                request.objective()));
    return new OnboardingResponse(
        UserController.UserResponse.from(result.profile()),
        WeightController.WeightResponse.from(result.weight()),
        result.needsProfile(),
        result.goals());
  }

  public record OnboardingRequest(
      @NotNull Sex sex,
      @NotNull LocalDate birthDate,
      @NotNull @Positive BigDecimal heightCm,
      @NotNull @Positive BigDecimal weightKg,
      @NotNull ActivityLevel activityLevel,
      @NotNull Objective objective) {}

  public record OnboardingResponse(
      UserController.UserResponse profile,
      WeightController.WeightResponse weight,
      boolean needsProfile,
      List<GoalsService.GoalResponse> goals) {}
}
