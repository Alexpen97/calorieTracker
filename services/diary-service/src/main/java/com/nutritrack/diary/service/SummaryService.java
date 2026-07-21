package com.nutritrack.diary.service;

import com.nutritrack.diary.client.UserGoalResponse;
import com.nutritrack.diary.client.UserGoalsClient;
import com.nutritrack.diary.domain.DiaryEntry;
import com.nutritrack.diary.domain.DiaryEntryNutrient;
import com.nutritrack.diary.domain.DiaryEntryRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SummaryService {

  private static final String WATER_NUTRIENT_CODE = "water_ml";

  private final DiaryEntryRepository entryRepository;
  private final WaterIntakeRepository waterRepository;
  private final UserGoalsClient userGoalsClient;

  public SummaryService(
      DiaryEntryRepository entryRepository,
      WaterIntakeRepository waterRepository,
      UserGoalsClient userGoalsClient) {
    this.entryRepository = entryRepository;
    this.waterRepository = waterRepository;
    this.userGoalsClient = userGoalsClient;
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
    return summarizeDate(date, entries, waterLogs, loadTargets(bearerToken));
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
    List<DailySummary> summaries = new ArrayList<>();
    for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
      summaries.add(
          summarizeDate(
              date,
              entriesByDate.getOrDefault(date, List.of()),
              waterByDate.getOrDefault(date, List.of()),
              targets));
    }
    return summaries;
  }

  private DailySummary summarizeDate(
      LocalDate date, List<DiaryEntry> entries, List<WaterIntake> waterLogs, GoalTargets targets) {
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
    List<NutrientTotal> nutrientTotals =
        totals.values().stream()
            .map(total -> total.toResponse(targets.nutrientTargets().get(total.code)))
            .sorted(Comparator.comparing(NutrientTotal::code))
            .toList();
    BigDecimal waterAmount =
        waterLogs.stream().map(WaterIntake::getAmountMl).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new DailySummary(
        date, nutrientTotals, new WaterSummary(waterAmount, targets.waterTargetMl()));
  }

  private GoalTargets loadTargets(String bearerToken) {
    try {
      List<UserGoalResponse> goals = userGoalsClient.getGoals(bearerToken);
      Map<String, BigDecimal> nutrientTargets = new HashMap<>();
      BigDecimal waterTarget = null;
      for (UserGoalResponse goal : goals) {
        if (WATER_NUTRIENT_CODE.equals(goal.nutrientCode())) {
          waterTarget = goal.dailyTarget();
        } else {
          nutrientTargets.put(goal.nutrientCode(), goal.dailyTarget());
        }
      }
      return new GoalTargets(Map.copyOf(nutrientTargets), waterTarget);
    } catch (RuntimeException ex) {
      return new GoalTargets(Map.of(), null);
    }
  }

  public record DailySummary(LocalDate date, List<NutrientTotal> totals, WaterSummary water) {}

  public record NutrientTotal(String code, BigDecimal amount, String unit, BigDecimal target) {}

  public record WaterSummary(BigDecimal amountMl, BigDecimal targetMl) {}

  private record GoalTargets(Map<String, BigDecimal> nutrientTargets, BigDecimal waterTargetMl) {}

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
