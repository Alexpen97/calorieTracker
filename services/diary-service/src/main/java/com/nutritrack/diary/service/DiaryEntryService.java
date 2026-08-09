package com.nutritrack.diary.service;

import com.nutritrack.diary.client.FoodCatalogClient;
import com.nutritrack.diary.client.ProductResponse;
import com.nutritrack.diary.domain.DiaryEntry;
import com.nutritrack.diary.domain.DiaryEntryNutrient;
import com.nutritrack.diary.domain.DiaryEntryRepository;
import com.nutritrack.diary.domain.MealType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiaryEntryService {

  static final int DEFAULT_FREQUENT_LIMIT = 8;
  static final int MAX_FREQUENT_LIMIT = 20;
  static final int DEFAULT_FREQUENT_WEEKS = 8;
  static final int MAX_FREQUENT_WEEKS = 52;
  private static final int MIN_LOG_COUNT = 2;

  private final DiaryEntryRepository entryRepository;
  private final FoodCatalogClient foodCatalogClient;

  public DiaryEntryService(
      DiaryEntryRepository entryRepository, FoodCatalogClient foodCatalogClient) {
    this.entryRepository = entryRepository;
    this.foodCatalogClient = foodCatalogClient;
  }

  @Transactional
  public DiaryEntry create(
      UUID userId,
      UUID productId,
      UUID submissionId,
      BigDecimal weightG,
      MealType mealType,
      Instant consumedAt,
      String bearerToken) {
    if ((productId == null && submissionId == null) || (productId != null && submissionId != null)) {
      throw new IllegalArgumentException("Provide exactly one of productId or submissionId");
    }
    UUID lookupId = productId != null ? productId : submissionId;
    ProductResponse product = foodCatalogClient.getProduct(lookupId, bearerToken);
    Instant now = Instant.now();
    DiaryEntry entry = new DiaryEntry();
    entry.setId(UUID.randomUUID());
    entry.setUserId(userId);

    boolean pendingSubmission = "PENDING_SUBMISSION".equals(product.source());
    if (pendingSubmission || submissionId != null) {
      entry.setSubmissionId(product.submissionId() != null ? product.submissionId() : submissionId);
      entry.setProductId(null);
    } else {
      entry.setProductId(product.id());
      entry.setSubmissionId(null);
    }

    entry.setProductName(product.name());
    entry.setBrand(product.brand());
    entry.setWeightG(weightG);
    entry.setMealType(mealType);
    entry.setConsumedAt(consumedAt == null ? now : consumedAt);
    entry.setCreatedAt(now);
    entry.replaceNutrients(snapshotNutrients(product, entry));
    return entryRepository.save(entry);
  }

  @Transactional(readOnly = true)
  public List<DiaryEntry> listByDate(UUID userId, LocalDate date, ZoneId zone) {
    Instant from = DayBounds.startOfDay(date, zone);
    Instant to = DayBounds.startOfNextDay(date, zone);
    return entryRepository
        .findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtDesc(
            userId, from, to);
  }

  /**
   * Aggregate frequently logged products in Java after the existing range query (avoids
   * Postgres-only SQL vs H2 test mismatches; personal diaries are small within the window).
   *
   * <p>Param rules: missing limit/weeks use defaults (8/8). Present but {@code < 1} or above caps
   * (20 / 52) → {@link IllegalArgumentException} (HTTP 400).
   */
  @Transactional(readOnly = true)
  public List<FrequentProduct> listFrequent(UUID userId, Integer limitParam, Integer weeksParam) {
    int limit = resolveFrequentLimit(limitParam);
    int weeks = resolveFrequentWeeks(weeksParam);
    Instant to = Instant.now().plusSeconds(1);
    Instant from = Instant.now().minus(Duration.ofDays(weeks * 7L));
    List<DiaryEntry> entries =
        entryRepository
            .findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtDesc(
                userId, from, to);

    Map<UUID, Acc> groups = new HashMap<>();
    for (DiaryEntry entry : entries) {
      UUID key = entry.getProductId() != null ? entry.getProductId() : entry.getSubmissionId();
      if (key == null) {
        continue;
      }
      Acc acc = groups.computeIfAbsent(key, ignored -> new Acc(entry.getProductId() != null));
      acc.count++;
      acc.weightSum += entry.getWeightG().doubleValue();
      if (acc.latest == null || entry.getConsumedAt().isAfter(acc.latest.getConsumedAt())) {
        acc.latest = entry;
      }
    }

    List<FrequentProduct> result = new ArrayList<>();
    for (Map.Entry<UUID, Acc> group : groups.entrySet()) {
      Acc acc = group.getValue();
      if (acc.count < MIN_LOG_COUNT || acc.latest == null) {
        continue;
      }
      UUID productId = acc.productKeyed ? group.getKey() : null;
      UUID submissionId = acc.productKeyed ? null : group.getKey();
      result.add(
          new FrequentProduct(
              productId,
              submissionId,
              acc.latest.getProductName(),
              acc.latest.getBrand(),
              acc.count,
              (int) Math.round(acc.weightSum / acc.count),
              acc.latest.getMealType(),
              acc.latest.getConsumedAt()));
    }

    result.sort(
        Comparator.comparingLong(FrequentProduct::logCount)
            .reversed()
            .thenComparing(FrequentProduct::lastConsumedAt, Comparator.reverseOrder()));
    if (result.size() > limit) {
      return List.copyOf(result.subList(0, limit));
    }
    return List.copyOf(result);
  }

  @Transactional
  public DiaryEntry update(
      UUID userId, UUID entryId, BigDecimal weightG, MealType mealType, Instant consumedAt) {
    DiaryEntry entry = requireEntry(userId, entryId);
    if (weightG != null) {
      entry.setWeightG(weightG);
    }
    if (mealType != null) {
      entry.setMealType(mealType);
    }
    if (consumedAt != null) {
      entry.setConsumedAt(consumedAt);
    }
    return entry;
  }

  @Transactional
  public void delete(UUID userId, UUID entryId) {
    DiaryEntry entry = requireEntry(userId, entryId);
    entryRepository.delete(entry);
  }

  private static int resolveFrequentLimit(Integer limitParam) {
    if (limitParam == null) {
      return DEFAULT_FREQUENT_LIMIT;
    }
    if (limitParam < 1 || limitParam > MAX_FREQUENT_LIMIT) {
      throw new IllegalArgumentException(
          "limit must be between 1 and " + MAX_FREQUENT_LIMIT + " (inclusive)");
    }
    return limitParam;
  }

  private static int resolveFrequentWeeks(Integer weeksParam) {
    if (weeksParam == null) {
      return DEFAULT_FREQUENT_WEEKS;
    }
    if (weeksParam < 1 || weeksParam > MAX_FREQUENT_WEEKS) {
      throw new IllegalArgumentException(
          "weeks must be between 1 and " + MAX_FREQUENT_WEEKS + " (inclusive)");
    }
    return weeksParam;
  }

  private DiaryEntry requireEntry(UUID userId, UUID entryId) {
    return entryRepository
        .findByIdAndUserId(entryId, userId)
        .orElseThrow(() -> new DiaryEntryNotFoundException(entryId));
  }

  private List<DiaryEntryNutrient> snapshotNutrients(ProductResponse product, DiaryEntry entry) {
    if (product.nutrients() == null) {
      return List.of();
    }
    return product.nutrients().stream()
        .map(nutrient -> snapshotNutrient(entry, nutrient))
        .toList();
  }

  private DiaryEntryNutrient snapshotNutrient(
      DiaryEntry entry, ProductResponse.NutrientResponse nutrient) {
    DiaryEntryNutrient snapshot = new DiaryEntryNutrient();
    snapshot.setEntry(entry);
    snapshot.setNutrientCode(nutrient.code());
    snapshot.setAmountPer100g(nutrient.amountPer100g());
    snapshot.setUnit(nutrient.unit());
    return snapshot;
  }

  public record FrequentProduct(
      UUID productId,
      UUID submissionId,
      String productName,
      String brand,
      long logCount,
      int usualWeightG,
      MealType lastMealType,
      Instant lastConsumedAt) {}

  private static final class Acc {
    final boolean productKeyed;
    long count;
    double weightSum;
    DiaryEntry latest;

    Acc(boolean productKeyed) {
      this.productKeyed = productKeyed;
    }
  }
}
