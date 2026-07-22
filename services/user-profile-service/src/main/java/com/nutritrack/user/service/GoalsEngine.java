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
  private static final BigDecimal CALORIES_PER_GRAM_PROTEIN = new BigDecimal("4");
  private static final BigDecimal CALORIES_PER_GRAM_CARBOHYDRATE = new BigDecimal("4");
  private static final BigDecimal CALORIES_PER_GRAM_FAT = new BigDecimal("9");

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
      MacroProfile macroProfile = macroProfile(user.getObjective());
      BigDecimal energyKcal = energyTarget(user, weightKg, userAge, macroProfile);
      BigDecimal proteinGrams =
          weightKg.multiply(macroProfile.proteinPerKg()).setScale(2, RoundingMode.HALF_UP);
      BigDecimal fatGrams =
          energyKcal
              .multiply(macroProfile.fatEnergyRatio())
              .divide(CALORIES_PER_GRAM_FAT, 2, RoundingMode.HALF_UP);
      BigDecimal carbohydratesGrams =
          energyKcal
              .subtract(proteinGrams.multiply(CALORIES_PER_GRAM_PROTEIN))
              .subtract(fatGrams.multiply(CALORIES_PER_GRAM_FAT))
              .divide(CALORIES_PER_GRAM_CARBOHYDRATE, 2, RoundingMode.HALF_UP);
      suggested.add(new SuggestedGoal("energy_kcal", energyKcal, "kcal"));
      suggested.add(
          new SuggestedGoal("protein", proteinGrams, "g"));
      suggested.add(new SuggestedGoal("carbohydrates", carbohydratesGrams, "g"));
      suggested.add(new SuggestedGoal("fat", fatGrams, "g"));
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

  private static BigDecimal energyTarget(
      AppUser user, BigDecimal weightKg, int age, MacroProfile macroProfile) {
    BigDecimal bmr =
        weightKg
            .multiply(TEN)
            .add(user.getHeightCm().multiply(SIX_POINT_TWO_FIVE))
            .subtract(FIVE.multiply(BigDecimal.valueOf(age)))
            .add(sexAdjustment(user.getSex()));
    return bmr.multiply(activityMultiplier(user.getActivityLevel()))
        .multiply(macroProfile.energyFactor())
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

  private static MacroProfile macroProfile(Objective objective) {
    return switch (objective) {
      case LOSE -> new MacroProfile("0.85", "1.6", "0.25");
      case CUT -> new MacroProfile("0.80", "2.0", "0.25");
      case MAINTAIN -> new MacroProfile("1.0", "1.2", "0.30");
      case GAIN -> new MacroProfile("1.10", "1.5", "0.30");
      case MUSCLE_GAIN -> new MacroProfile("1.10", "2.0", "0.25");
      case BULK -> new MacroProfile("1.20", "1.8", "0.25");
    };
  }

  private record MacroProfile(
      BigDecimal energyFactor, BigDecimal proteinPerKg, BigDecimal fatEnergyRatio) {
    MacroProfile(String energyFactor, String proteinPerKg, String fatEnergyRatio) {
      this(new BigDecimal(energyFactor), new BigDecimal(proteinPerKg), new BigDecimal(fatEnergyRatio));
    }
  }

  public record Result(boolean needsProfile, List<SuggestedGoal> suggested) {}

  public record SuggestedGoal(String nutrientCode, BigDecimal dailyTarget, String unit) {}
}
