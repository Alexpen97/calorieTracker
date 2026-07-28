package com.nutritrack.diary.service;

import com.nutritrack.diary.client.UserGoalResponse;
import com.nutritrack.diary.client.UserGoalsClient;
import com.nutritrack.diary.domain.DiaryEntry;
import com.nutritrack.diary.domain.DiaryEntryNutrient;
import com.nutritrack.diary.domain.DiaryEntryRepository;
import com.nutritrack.diary.domain.HealthActivityProvider;
import com.nutritrack.diary.domain.WaterIntake;
import com.nutritrack.diary.domain.WaterIntakeRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SummaryService {

  private static final Logger log = LoggerFactory.getLogger(SummaryService.class);
  private static final String WATER_NUTRIENT_CODE = "water_ml";
  private static final String ENERGY_NUTRIENT_CODE = "energy_kcal";

  private final DiaryEntryRepository entryRepository;
  private final WaterIntakeRepository waterRepository;
  private final UserGoalsClient userGoalsClient;
  private final ActivityEnergyService activityEnergyService;

  public SummaryService(
      DiaryEntryRepository entryRepository,
      WaterIntakeRepository waterRepository,
      UserGoalsClient userGoalsClient,
      ActivityEnergyService activityEnergyService) {
    this.entryRepository = entryRepository;
    this.waterRepository = waterRepository;
    this.userGoalsClient = userGoalsClient;
    this.activityEnergyService = activityEnergyService;
  }

  @Transactional(readOnly = true)
  public DailySummary summarize(UUID userId, LocalDate date, ZoneId zone, String bearerToken) {
    Instant from = DayBounds.startOfDay(date, zone);
    Instant to = DayBounds.startOfNextDay(date, zone);
    List<DiaryEntry> entries =
        entryRepository
            .findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtDesc(
                userId, from, to);
    List<WaterIntake> waterLogs =
        waterRepository
            .findByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtDesc(
                userId, from, to);
    return summarizeDate(
        userId, date, zone, entries, waterLogs, loadTargets(bearerToken), Map.of());
  }

  @Transactional(readOnly = true)
  public List<DailySummary> summarizeRange(
      UUID userId, LocalDate fromDate, LocalDate toDate, ZoneId zone, String bearerToken) {
    if (toDate.isBefore(fromDate)) {
      return List.of();
    }
    Instant from = DayBounds.startOfDay(fromDate, zone);
    Instant to = DayBounds.startOfNextDay(toDate, zone);
    Map<LocalDate, List<DiaryEntry>> entriesByDate =
        entryRepository
            .findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtDesc(
                userId, from, to)
            .stream()
            .collect(Collectors.groupingBy(entry -> DayBounds.localDate(entry.getConsumedAt(), zone)));
    Map<LocalDate, List<WaterIntake>> waterByDate =
        waterRepository
            .findByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtDesc(
                userId, from, to)
            .stream()
            .collect(Collectors.groupingBy(water -> DayBounds.localDate(water.getLoggedAt(), zone)));
    GoalTargets targets = loadTargets(bearerToken);
    Map<LocalDate, ActivityEnergyService.BurnedEnergy> burnedByDate =
        loadBurnedRange(userId, fromDate, toDate);
    List<DailySummary> summaries = new ArrayList<>();
    for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
      summaries.add(
          summarizeDate(
              userId,
              date,
              zone,
              entriesByDate.getOrDefault(date, List.of()),
              waterByDate.getOrDefault(date, List.of()),
              targets,
              burnedByDate));
    }
    return summaries;
  }

  private DailySummary summarizeDate(
      UUID userId,
      LocalDate date,
      ZoneId zone,
      List<DiaryEntry> entries,
      List<WaterIntake> waterLogs,
      GoalTargets targets,
      Map<LocalDate, ActivityEnergyService.BurnedEnergy> burnedPrefetch) {
    Map<String, MutableNutrientTotal> totals = new HashMap<>();
    for (DiaryEntry entry : entries) {
      for (DiaryEntryNutrient nutrient : entry.getNutrients()) {
        BigDecimal amount = PortionMath.scale(nutrient.getAmountPer100g(), entry.getWeightG());
        totals
            .computeIfAbsent(
                nutrient.getNutrientCode(),
                code -> new MutableNutrientTotal(code, nutrient.getUnit()))
            .add(amount);
      }
    }
    for (Map.Entry<String, GoalNutrient> goal : targets.nutrients().entrySet()) {
      totals.computeIfAbsent(
          goal.getKey(), code -> new MutableNutrientTotal(code, goal.getValue().unit()));
    }
    List<NutrientTotal> nutrientTotals =
        totals.values().stream()
            .map(
                total -> {
                  GoalNutrient goal = targets.nutrients().get(total.code);
                  return total.toResponse(goal == null ? null : goal.target());
                })
            .sorted(Comparator.comparing(NutrientTotal::code))
            .toList();
    BigDecimal waterAmount =
        waterLogs.stream().map(WaterIntake::getAmountMl).reduce(BigDecimal.ZERO, BigDecimal::add);
    EnergyAdjustment energyAdjustment =
        buildEnergyAdjustment(userId, date, zone, targets, burnedPrefetch);
    return new DailySummary(
        date,
        nutrientTotals,
        new WaterSummary(waterAmount, targets.waterTargetMl()),
        energyAdjustment);
  }

  private EnergyAdjustment buildEnergyAdjustment(
      UUID userId,
      LocalDate date,
      ZoneId zone,
      GoalTargets targets,
      Map<LocalDate, ActivityEnergyService.BurnedEnergy> burnedPrefetch) {
    GoalNutrient energyGoal = targets.nutrients().get(ENERGY_NUTRIENT_CODE);
    if (energyGoal == null || energyGoal.target() == null) {
      return null;
    }
    ActivityEnergyService.BurnedEnergy burned = burnedPrefetch.get(date);
    if (burned == null && burnedPrefetch.isEmpty()) {
      burned = loadBurned(userId, date, zone.getId());
    }
    if (burned == null || burned.burnedCalories() == null) {
      return null;
    }
    BigDecimal baseTarget = energyGoal.target();
    BigDecimal burnedCalories = burned.burnedCalories();
    return new EnergyAdjustment(
        burned.provider(),
        burnedCalories,
        baseTarget,
        baseTarget.add(burnedCalories),
        burned.syncedAt());
  }

  private ActivityEnergyService.BurnedEnergy loadBurned(UUID userId, LocalDate date, String zoneId) {
    try {
      return activityEnergyService
          .findBurnedEnergy(userId, date, zoneId, HealthActivityProvider.SAMSUNG_HEALTH)
          .orElse(null);
    } catch (RuntimeException ex) {
      log.warn("Diary summary could not load activity energy; omitting adjustment", ex);
      return null;
    }
  }

  private Map<LocalDate, ActivityEnergyService.BurnedEnergy> loadBurnedRange(
      UUID userId, LocalDate from, LocalDate to) {
    try {
      return activityEnergyService.findBurnedEnergyRange(
          userId, from, to, HealthActivityProvider.SAMSUNG_HEALTH);
    } catch (RuntimeException ex) {
      log.warn("Diary range summary could not load activity energy; omitting adjustments", ex);
      return Map.of();
    }
  }

  private GoalTargets loadTargets(String bearerToken) {
    try {
      List<UserGoalResponse> goals = userGoalsClient.getGoals(bearerToken);
      Map<String, GoalNutrient> nutrients = new HashMap<>();
      BigDecimal waterTarget = null;
      for (UserGoalResponse goal : goals) {
        if (WATER_NUTRIENT_CODE.equals(goal.nutrientCode())) {
          waterTarget = goal.dailyTarget();
        } else {
          nutrients.put(goal.nutrientCode(), new GoalNutrient(goal.dailyTarget(), goal.unit()));
        }
      }
      return new GoalTargets(Map.copyOf(nutrients), waterTarget);
    } catch (RuntimeException ex) {
      log.warn("Diary summary could not load user goals; returning null targets", ex);
      return new GoalTargets(Map.of(), null);
    }
  }

  public record DailySummary(
      LocalDate date,
      List<NutrientTotal> totals,
      WaterSummary water,
      EnergyAdjustment energyAdjustment) {}

  public record NutrientTotal(String code, BigDecimal amount, String unit, BigDecimal target) {}

  public record WaterSummary(BigDecimal amountMl, BigDecimal targetMl) {}

  public record EnergyAdjustment(
      String provider,
      BigDecimal burnedCalories,
      BigDecimal baseTarget,
      BigDecimal effectiveTarget,
      Instant syncedAt) {}

  private record GoalNutrient(BigDecimal target, String unit) {}

  private record GoalTargets(Map<String, GoalNutrient> nutrients, BigDecimal waterTargetMl) {}

  private static class MutableNutrientTotal {
    private final String code;
    private final String unit;
    private BigDecimal amount = BigDecimal.ZERO;

    private MutableNutrientTotal(String code, String unit) {
      this.code = code;
      this.unit = unit;
    }

    private void add(BigDecimal nextAmount) {
      amount = amount.add(nextAmount);
    }

    private NutrientTotal toResponse(BigDecimal target) {
      return new NutrientTotal(code, amount, unit, target);
    }
  }
}
