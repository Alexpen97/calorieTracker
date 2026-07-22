package com.nutritrack.user.service;

import com.nutritrack.user.domain.ActivityLevel;
import com.nutritrack.user.domain.AppUser;
import com.nutritrack.user.domain.BodyWeightLog;
import com.nutritrack.user.domain.Objective;
import com.nutritrack.user.domain.Sex;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {

  private final UserService userService;
  private final WeightService weightService;
  private final GoalsService goalsService;

  public OnboardingService(
      UserService userService, WeightService weightService, GoalsService goalsService) {
    this.userService = userService;
    this.weightService = weightService;
    this.goalsService = goalsService;
  }

  @Transactional
  public OnboardingResult complete(UUID userId, OnboardingInput input) {
    AppUser profile =
        userService.updateProfile(
            userId,
            new UserService.ProfileUpdate(
                null,
                input.sex(),
                input.birthDate(),
                input.heightCm(),
                input.activityLevel(),
                input.objective()));
    BodyWeightLog weight = weightService.create(userId, input.weightKg(), null);
    GoalsService.RecalculateResponse goals = goalsService.recalculate(userId, true);
    return new OnboardingResult(profile, weight, goals.needsProfile(), goals.current());
  }

  public record OnboardingInput(
      Sex sex,
      LocalDate birthDate,
      BigDecimal heightCm,
      BigDecimal weightKg,
      ActivityLevel activityLevel,
      Objective objective) {}

  public record OnboardingResult(
      AppUser profile,
      BodyWeightLog weight,
      boolean needsProfile,
      List<GoalsService.GoalResponse> goals) {}
}
