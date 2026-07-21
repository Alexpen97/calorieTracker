package com.nutritrack.diary.service;

import com.nutritrack.diary.client.FoodCatalogClient;
import com.nutritrack.diary.client.ProductResponse;
import com.nutritrack.diary.domain.DiaryEntry;
import com.nutritrack.diary.domain.DiaryEntryNutrient;
import com.nutritrack.diary.domain.DiaryEntryRepository;
import com.nutritrack.diary.domain.MealType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiaryEntryService {

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
      BigDecimal weightG,
      MealType mealType,
      Instant consumedAt,
      String bearerToken) {
    ProductResponse product = foodCatalogClient.getProduct(productId, bearerToken);
    Instant now = Instant.now();
    DiaryEntry entry = new DiaryEntry();
    entry.setId(UUID.randomUUID());
    entry.setUserId(userId);
    entry.setProductId(product.id());
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
  public List<DiaryEntry> listByDate(UUID userId, LocalDate date) {
    Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant to = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    return entryRepository
        .findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtDesc(
            userId, from, to);
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
}
