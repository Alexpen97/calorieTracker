package com.nutritrack.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutritrack.user.domain.ActivityLevel;
import com.nutritrack.user.domain.AppUser;
import com.nutritrack.user.domain.NutrientReferenceIntake;
import com.nutritrack.user.domain.Objective;
import com.nutritrack.user.domain.Sex;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GoalsEngineTest {

  private final GoalsEngine engine = new GoalsEngine();

  @Test
  void calculatesMaleMaintainGoldenTargets() {
    AppUser user = completeUser(Sex.MALE, LocalDate.of(1996, 7, 21), new BigDecimal("180"));
    user.setActivityLevel(ActivityLevel.MODERATE);
    user.setObjective(Objective.MAINTAIN);

    GoalsEngine.Result result =
        engine.calculate(
            user,
            Optional.of(new BigDecimal("80")),
            List.of(reference("fiber", Sex.MALE, "35", "g", "FIXED")),
            LocalDate.of(2026, 7, 21));

    assertThat(result.needsProfile()).isFalse();
    assertThat(target(result, "energy_kcal").dailyTarget()).isEqualByComparingTo("2759.00");
    assertThat(target(result, "protein").dailyTarget()).isEqualByComparingTo("96.00");
    assertThat(target(result, "water_ml").dailyTarget()).isEqualByComparingTo("2800.00");
    assertThat(target(result, "fiber").dailyTarget()).isEqualByComparingTo("35.00");
  }

  @Test
  void marksProfileIncompleteAndSkipsBodyTargetsWhenMissingWeightHeightOrActivity() {
    AppUser user = new AppUser();
    user.setSex(Sex.FEMALE);
    user.setBirthDate(LocalDate.of(1990, 1, 1));

    GoalsEngine.Result result =
        engine.calculate(
            user,
            Optional.empty(),
            List.of(reference("iron", Sex.FEMALE, "18", "mg", "FIXED")),
            LocalDate.of(2026, 7, 21));

    assertThat(result.needsProfile()).isTrue();
    assertThat(result.suggested()).extracting(GoalsEngine.SuggestedGoal::nutrientCode)
        .containsExactly("iron");
    assertThat(target(result, "iron").dailyTarget()).isEqualByComparingTo("18.00");
  }

  @Test
  void appliesObjectiveSpecificProteinAndEnergyFactors() {
    AppUser user = completeUser(Sex.FEMALE, LocalDate.of(1986, 7, 21), new BigDecimal("165"));
    user.setActivityLevel(ActivityLevel.LIGHT);
    user.setObjective(Objective.LOSE);

    GoalsEngine.Result result =
        engine.calculate(
            user,
            Optional.of(new BigDecimal("70")),
            List.of(reference("protein", Sex.FEMALE, "0.83", "g", "PER_KG")),
            LocalDate.of(2026, 7, 21));

    assertThat(target(result, "energy_kcal").dailyTarget()).isEqualByComparingTo("1601.48");
    assertThat(target(result, "protein").dailyTarget()).isEqualByComparingTo("112.00");
    assertThat(result.suggested()).extracting(GoalsEngine.SuggestedGoal::nutrientCode)
        .containsOnly("energy_kcal", "protein", "water_ml");
  }

  private static AppUser completeUser(Sex sex, LocalDate birthDate, BigDecimal heightCm) {
    AppUser user = new AppUser();
    user.setSex(sex);
    user.setBirthDate(birthDate);
    user.setHeightCm(heightCm);
    return user;
  }

  private static NutrientReferenceIntake reference(
      String code, Sex sex, String amount, String unit, String basis) {
    NutrientReferenceIntake reference = new NutrientReferenceIntake();
    reference.setNutrientCode(code);
    reference.setSex(sex);
    reference.setAgeMin(19);
    reference.setAgeMax(50);
    reference.setDailyAmount(new BigDecimal(amount));
    reference.setUnit(unit);
    reference.setBasis(basis);
    return reference;
  }

  private static GoalsEngine.SuggestedGoal target(GoalsEngine.Result result, String nutrientCode) {
    return result.suggested().stream()
        .filter(goal -> goal.nutrientCode().equals(nutrientCode))
        .findFirst()
        .orElseThrow();
  }
}
