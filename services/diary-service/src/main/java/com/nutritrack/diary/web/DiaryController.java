package com.nutritrack.diary.web;

import com.nutritrack.diary.domain.DiaryEntry;
import com.nutritrack.diary.domain.DiaryEntryNutrient;
import com.nutritrack.diary.domain.MealType;
import com.nutritrack.diary.service.DiaryEntryService;
import com.nutritrack.diary.service.PortionMath;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiaryController {

  private final DiaryEntryService entryService;

  public DiaryController(DiaryEntryService entryService) {
    this.entryService = entryService;
  }

  @PostMapping("/api/diary/entries")
  public DiaryEntryResponse createEntry(
      @AuthenticationPrincipal Jwt jwt,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @Valid @RequestBody CreateDiaryEntryRequest request) {
    DiaryEntry entry =
        entryService.create(
            UUID.fromString(jwt.getSubject()),
            request.productId(),
            request.weightG(),
            request.mealType(),
            request.consumedAt(),
            authorization);
    return DiaryEntryResponse.from(entry);
  }

  @GetMapping("/api/diary/entries")
  public List<DiaryEntryResponse> listEntries(
      @AuthenticationPrincipal Jwt jwt, @RequestParam LocalDate date) {
    return entryService.listByDate(UUID.fromString(jwt.getSubject()), date).stream()
        .map(DiaryEntryResponse::from)
        .toList();
  }

  @PutMapping("/api/diary/entries/{id}")
  public DiaryEntryResponse updateEntry(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable("id") UUID id,
      @Valid @RequestBody UpdateDiaryEntryRequest request) {
    DiaryEntry entry =
        entryService.update(
            UUID.fromString(jwt.getSubject()),
            id,
            request.weightG(),
            request.mealType(),
            request.consumedAt());
    return DiaryEntryResponse.from(entry);
  }

  @DeleteMapping("/api/diary/entries/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
    entryService.delete(UUID.fromString(jwt.getSubject()), id);
  }

  public record CreateDiaryEntryRequest(
      @NotNull UUID productId,
      @NotNull @Positive BigDecimal weightG,
      @NotNull MealType mealType,
      Instant consumedAt) {}

  public record UpdateDiaryEntryRequest(
      @Positive BigDecimal weightG, MealType mealType, Instant consumedAt) {}

  public record DiaryEntryResponse(
      UUID id,
      UUID productId,
      String productName,
      String brand,
      BigDecimal weightG,
      MealType mealType,
      Instant consumedAt,
      Instant createdAt,
      List<NutrientAmountResponse> nutrients) {
    static DiaryEntryResponse from(DiaryEntry entry) {
      return new DiaryEntryResponse(
          entry.getId(),
          entry.getProductId(),
          entry.getProductName(),
          entry.getBrand(),
          entry.getWeightG(),
          entry.getMealType(),
          entry.getConsumedAt(),
          entry.getCreatedAt(),
          entry.getNutrients().stream()
              .sorted(Comparator.comparing(DiaryEntryNutrient::getNutrientCode))
              .map(nutrient -> NutrientAmountResponse.from(nutrient, entry.getWeightG()))
              .toList());
    }
  }

  public record NutrientAmountResponse(
      String code, BigDecimal amount, BigDecimal amountPer100g, String unit) {
    static NutrientAmountResponse from(DiaryEntryNutrient nutrient, BigDecimal weightG) {
      return new NutrientAmountResponse(
          nutrient.getNutrientCode(),
          PortionMath.scale(nutrient.getAmountPer100g(), weightG),
          nutrient.getAmountPer100g(),
          nutrient.getUnit());
    }
  }
}
