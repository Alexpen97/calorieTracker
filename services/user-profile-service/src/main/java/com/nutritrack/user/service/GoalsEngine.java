package com.nutritrack.user.service;

import com.nutritrack.user.domain.ActivityLevel;
import com.nutritrack.user.domain.AppUser;
import com.nutritrack.user.domain.NutrientReferenceIntake;
import com.nutritrack.user.domain.Objective;
import com.nutritrack.user.domain.Sex;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class GoalsEngine {

  private static final BigDecimal TEN = new BigDecimal("10");
  private static final BigDecimal SIX_POINT_TWO_FIVE = new BigDecimal("6.25");
  private static final BigDecimal FIVE = new BigDecimal("5");
  private static final BigDecimal WATER_ML_PER_KG = new BigDecimal("35");

  public Result calculate(
      AppUser user,
      Optional<BigDecimal> latestWeightKg,
      List<NutrientReferenceIntake> references,
      LocalDate today) {
    Optional<Integer> age = age(user, today);
    boolean needsProfile =
        user.getSex() == null
            || user.getHeightCm() == null
            || latestWeightKg.isEmpty()
            || user.getBirthDate() == null
            || user.getActivityLevel() == null
            || user.getObjective() == null;

    List<SuggestedGoal> suggested = new ArrayList<>();
    if (!needsProfile) {
      BigDecimal weightKg = latestWeightKg.orElseThrow();
      int userAge = age.orElseThrow();
      suggested.add(new SuggestedGoal("energy_kcal", energyTarget(user, weightKg, userAge), "kcal"));
      suggested.add(
          new SuggestedGoal(
              "protein",
              weightKg.multiply(proteinPerKg(user.getObjective())).setScale(2, RoundingMode.HALF_UP),
              "g"));
      suggested.add(
          new SuggestedGoal(
              "water_ml",
              weightKg.multiply(WATER_ML_PER_KG).setScale(2, RoundingMode.HALF_UP),
              "ml"));
    }

    for (NutrientReferenceIntake reference : references) {
      if ("protein".equals(reference.getNutrientCode())) {
        continue;
      }
      if ("FIXED".equals(reference.getBasis())) {
        suggested.add(
            new SuggestedGoal(
                reference.getNutrientCode(),
                reference.getDailyAmount().setScale(2, RoundingMode.HALF_UP),
                reference.getUnit()));
      } else if ("PER_KG".equals(reference.getBasis()) && latestWeightKg.isPresent()) {
        suggested.add(
            new SuggestedGoal(
                reference.getNutrientCode(),
                reference
                    .getDailyAmount()
                    .multiply(latestWeightKg.orElseThrow())
                    .setScale(2, RoundingMode.HALF_UP),
                reference.getUnit()));
      }
    }

    suggested.sort(Comparator.comparing(SuggestedGoal::nutrientCode));
    return new Result(needsProfile, List.copyOf(suggested));
  }

  private static Optional<Integer> age(AppUser user, LocalDate today) {
    if (user.getBirthDate() == null) {
      return Optional.empty();
    }
    return Optional.of(Period.between(user.getBirthDate(), today).getYears());
  }

  private static BigDecimal energyTarget(AppUser user, BigDecimal weightKg, int age) {
    BigDecimal bmr =
        weightKg
            .multiply(TEN)
            .add(user.getHeightCm().multiply(SIX_POINT_TWO_FIVE))
            .subtract(FIVE.multiply(BigDecimal.valueOf(age)))
            .add(sexAdjustment(user.getSex()));
    return bmr.multiply(activityMultiplier(user.getActivityLevel()))
        .multiply(objectiveFactor(user.getObjective()))
        .setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal sexAdjustment(Sex sex) {
    return sex == Sex.MALE ? new BigDecimal("5") : new BigDecimal("-161");
  }

  private static BigDecimal activityMultiplier(ActivityLevel activityLevel) {
    return switch (activityLevel) {
      case SEDENTARY -> new BigDecimal("1.2");
      case LIGHT -> new BigDecimal("1.375");
      case MODERATE -> new BigDecimal("1.55");
      case ACTIVE -> new BigDecimal("1.725");
      case VERY_ACTIVE -> new BigDecimal("1.9");
    };
  }

  private static BigDecimal objectiveFactor(Objective objective) {
    return switch (objective) {
      case LOSE -> new BigDecimal("0.85");
      case MAINTAIN -> new BigDecimal("1.0");
      case GAIN -> new BigDecimal("1.15");
    };
  }

  private static BigDecimal proteinPerKg(Objective objective) {
    return switch (objective) {
      case LOSE -> new BigDecimal("1.6");
      case MAINTAIN -> new BigDecimal("1.2");
      case GAIN -> new BigDecimal("1.8");
    };
  }

  public record Result(boolean needsProfile, List<SuggestedGoal> suggested) {}

  public record SuggestedGoal(String nutrientCode, BigDecimal dailyTarget, String unit) {}
}
