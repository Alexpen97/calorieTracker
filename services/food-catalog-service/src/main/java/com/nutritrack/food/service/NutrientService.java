package com.nutritrack.food.service;

import com.nutritrack.food.domain.Nutrient;
import com.nutritrack.food.domain.NutrientRepository;
import com.nutritrack.food.web.NutrientNotFoundException;
import com.nutritrack.food.web.dto.NutrientResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NutrientService {

  private final NutrientRepository nutrientRepository;

  public NutrientService(NutrientRepository nutrientRepository) {
    this.nutrientRepository = nutrientRepository;
  }

  @Transactional(readOnly = true)
  public List<NutrientResponse> listAll() {
    return nutrientRepository.findAllByOrderByCategoryAscDisplayNameAsc().stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public NutrientResponse getByCode(String code) {
    Nutrient nutrient =
        nutrientRepository
            .findById(code)
            .orElseThrow(() -> new NutrientNotFoundException(code));
    return toResponse(nutrient);
  }

  private NutrientResponse toResponse(Nutrient nutrient) {
    return new NutrientResponse(
        nutrient.getCode(),
        nutrient.getDisplayName(),
        nutrient.getCategory().name(),
        nutrient.getDefaultUnit(),
        nutrient.getDescription(),
        nutrient.getBodyEffects(),
        nutrient.getDeficiencyEffects(),
        nutrient.getExcessEffects(),
        nutrient.getCommonSources(),
        nutrient.getContentSource());
  }
}
