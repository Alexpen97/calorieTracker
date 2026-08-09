package com.nutritrack.nevo.web;

import com.nutritrack.nevo.domain.NevoFood;
import com.nutritrack.nevo.domain.NevoFoodRepository;
import com.nutritrack.nevo.domain.NevoNutrientValueRepository;
import com.nutritrack.nevo.search.NevoFoodSearchService;
import com.nutritrack.nevo.web.dto.NevoFoodSearchResponse;
import com.nutritrack.nevo.web.dto.NevoMatchResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/nevo")
public class NevoFoodController {

  private final NevoFoodRepository foodRepository;
  private final NevoNutrientValueRepository nutrientRepository;
  private final NevoFoodSearchService searchService;

  public NevoFoodController(
      NevoFoodRepository foodRepository,
      NevoNutrientValueRepository nutrientRepository,
      NevoFoodSearchService searchService) {
    this.foodRepository = foodRepository;
    this.nutrientRepository = nutrientRepository;
    this.searchService = searchService;
  }

  @GetMapping("/foods/search")
  public NevoFoodSearchResponse search(
      @RequestParam("q") String q, @RequestParam(value = "limit", defaultValue = "10") int limit) {
    return searchService.search(q, Math.min(Math.max(limit, 1), 25));
  }

  @GetMapping("/foods/{nevoCode}")
  public NevoMatchResponse byCode(@PathVariable String nevoCode) {
    NevoFood food =
        foodRepository
            .findById(nevoCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NEVO food not found"));
    List<NevoMatchResponse.NevoNutrientDto> nutrients =
        nutrientRepository.findByNevoCode(nevoCode).stream()
            .filter(n -> n.getNutrientCode() != null && n.getAmountPer100g() != null)
            .map(
                n ->
                    new NevoMatchResponse.NevoNutrientDto(
                        n.getNutrientCode(), n.getAmountPer100g(), n.getUnit()))
            .toList();
    return new NevoMatchResponse(
        true,
        food.getNevoCode(),
        food.getFoodNameEn(),
        food.getFoodGroup(),
        food.getNevoVersion(),
        "NONE",
        1.0,
        List.of("directLookup"),
        nutrients);
  }
}
