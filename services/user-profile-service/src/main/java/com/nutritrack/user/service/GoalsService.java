package com.nutritrack.user.service;

import com.nutritrack.user.domain.AppUser;
import com.nutritrack.user.domain.AppUserRepository;
import com.nutritrack.user.domain.BodyWeightLogRepository;
import com.nutritrack.user.domain.GoalOrigin;
import com.nutritrack.user.domain.NutrientReferenceIntake;
import com.nutritrack.user.domain.NutrientReferenceIntakeRepository;
import com.nutritrack.user.domain.UserGoal;
import com.nutritrack.user.domain.UserGoalRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoalsService {

  private final AppUserRepository userRepository;
  private final BodyWeightLogRepository weightRepository;
  private final NutrientReferenceIntakeRepository referenceRepository;
  private final UserGoalRepository goalRepository;
  private final GoalsEngine goalsEngine;

  public GoalsService(
      AppUserRepository userRepository,
      BodyWeightLogRepository weightRepository,
      NutrientReferenceIntakeRepository referenceRepository,
      UserGoalRepository goalRepository,
      GoalsEngine goalsEngine) {
    this.userRepository = userRepository;
    this.weightRepository = weightRepository;
    this.referenceRepository = referenceRepository;
    this.goalRepository = goalRepository;
    this.goalsEngine = goalsEngine;
  }

  @Transactional
  public List<GoalResponse> list(UUID userId) {
    AppUser user = requireUser(userId);
    List<GoalResponse> current = currentGoals(userId);
    if (needsMicroBackfill(user, current)) {
      recalculate(userId, true);
      return currentGoals(userId);
    }
    return current;
  }

  @Transactional
  public List<GoalResponse> override(UUID userId, List<GoalOverride> overrides) {
    AppUser user = requireUser(userId);
    for (GoalOverride override : overrides) {
      UserGoal goal =
          goalRepository
              .findByUserIdAndNutrientCode(userId, override.nutrientCode())
              .orElseGet(
                  () -> {
                    UserGoal created = new UserGoal();
                    created.setUser(user);
                    created.setNutrientCode(override.nutrientCode());
                    return created;
                  });
      goal.setDailyTarget(override.dailyTarget());
      goal.setUnit(override.unit());
      goal.setOrigin(GoalOrigin.USER_OVERRIDE);
      goal.setComputedAt(null);
      goalRepository.save(goal);
    }
    return currentGoals(userId);
  }

  @Transactional
  public RecalculateResponse recalculate(UUID userId, boolean apply) {
    AppUser user = requireUser(userId);
    Optional<BigDecimal> latestWeight =
        weightRepository.findFirstByUser_IdOrderByMeasuredAtDesc(userId)
            .map(log -> log.getWeightKg());
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    List<NutrientReferenceIntake> references = referencesFor(user, today);
    GoalsEngine.Result result = goalsEngine.calculate(user, latestWeight, references, today);
    List<GoalResponse> suggested =
        result.suggested().stream().map(GoalsService::suggestedGoal).toList();

    if (apply) {
      applyComputed(user, suggested);
    }

    return new RecalculateResponse(result.needsProfile(), suggested, currentGoals(userId));
  }

  private void applyComputed(AppUser user, List<GoalResponse> suggested) {
    UUID userId = user.getId();
    Instant computedAt = Instant.now();
    Map<String, UserGoal> current =
        goalRepository.findByUserIdOrderByNutrientCode(userId).stream()
            .collect(Collectors.toMap(UserGoal::getNutrientCode, Function.identity()));

    for (GoalResponse suggestion : suggested) {
      UserGoal existing = current.get(suggestion.nutrientCode());
      if (existing != null && existing.getOrigin() == GoalOrigin.USER_OVERRIDE) {
        continue;
      }
      UserGoal goal = existing == null ? new UserGoal() : existing;
      if (existing == null) {
        goal.setUser(user);
        goal.setNutrientCode(suggestion.nutrientCode());
      }
      goal.setDailyTarget(suggestion.dailyTarget());
      goal.setUnit(suggestion.unit());
      goal.setOrigin(GoalOrigin.COMPUTED);
      goal.setComputedAt(computedAt);
      goalRepository.save(goal);
    }
  }

  private List<NutrientReferenceIntake> referencesFor(AppUser user, LocalDate today) {
    if (user.getSex() == null || user.getBirthDate() == null) {
      return List.of();
    }
    int age = java.time.Period.between(user.getBirthDate(), today).getYears();
    short ageForLookup = (short) age;
    List<NutrientReferenceIntake> exact =
        referenceRepository.findBySexAndAgeMinLessThanEqualAndAgeMaxGreaterThanEqual(
            user.getSex(), ageForLookup, ageForLookup);
    if (!exact.isEmpty()) {
      return exact;
    }
    // Seeded DRVs currently cover adults 19–50; fall back so other ages still get micros.
    return referenceRepository.findBySexAndAgeMinLessThanEqualAndAgeMaxGreaterThanEqual(
        user.getSex(), (short) 19, (short) 19);
  }

  private static boolean needsMicroBackfill(AppUser user, List<GoalResponse> current) {
    if (user.getSex() == null || user.getBirthDate() == null) {
      return false;
    }
    java.util.Set<String> codes =
        current.stream().map(GoalResponse::nutrientCode).collect(Collectors.toSet());
    // Sentinel codes from the expanded DRV set; missing any means pre-expand goals.
    return !codes.contains("vitamin_a")
        || !codes.contains("iron")
        || !codes.contains("selenium")
        || !codes.contains("vitamin_b9");
  }

  private List<GoalResponse> currentGoals(UUID userId) {
    return goalRepository.findByUserIdOrderByNutrientCode(userId).stream()
        .map(GoalsService::currentGoal)
        .toList();
  }

  private static GoalResponse currentGoal(UserGoal goal) {
    return new GoalResponse(
        goal.getNutrientCode(),
        goal.getDailyTarget(),
        goal.getUnit(),
        goal.getOrigin(),
        goal.getComputedAt());
  }

  private static GoalResponse suggestedGoal(GoalsEngine.SuggestedGoal goal) {
    return new GoalResponse(
        goal.nutrientCode(), goal.dailyTarget(), goal.unit(), GoalOrigin.COMPUTED, null);
  }

  private AppUser requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

  public record GoalOverride(String nutrientCode, BigDecimal dailyTarget, String unit) {}

  public record GoalResponse(
      String nutrientCode,
      BigDecimal dailyTarget,
      String unit,
      GoalOrigin origin,
      Instant computedAt) {}

  public record RecalculateResponse(
      boolean needsProfile, List<GoalResponse> suggested, List<GoalResponse> current) {}
}
