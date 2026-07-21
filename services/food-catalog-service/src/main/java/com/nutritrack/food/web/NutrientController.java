package com.nutritrack.food.web;

import com.nutritrack.food.service.NutrientService;
import com.nutritrack.food.web.dto.NutrientResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nutrients")
public class NutrientController {

  private final NutrientService nutrientService;

  public NutrientController(NutrientService nutrientService) {
    this.nutrientService = nutrientService;
  }

  @GetMapping
  public List<NutrientResponse> list() {
    return nutrientService.listAll();
  }

  @GetMapping("/{code}")
  public NutrientResponse byCode(@PathVariable("code") String code) {
    return nutrientService.getByCode(code);
  }
}
