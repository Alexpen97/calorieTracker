package com.nutritrack.food.service;

import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductNutrient;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.nevo.NevoClient;
import com.nutritrack.food.nevo.NevoMatchRequest;
import com.nutritrack.food.nevo.NevoMatchResponse;
import com.nutritrack.food.nevo.NevoMicronutrientCodes;
import com.nutritrack.food.nevo.NevoUnavailableException;
import com.nutritrack.food.config.FoodProperties;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NevoEnrichmentService {

  private static final Logger log = LoggerFactory.getLogger(NevoEnrichmentService.class);
  private static final Set<String> AUTO_APPLY = Set.of("HIGH", "MEDIUM");

  private final NevoClient nevoClient;
  private final ProductRepository productRepository;
  private final FoodProperties properties;

  public NevoEnrichmentService(
      NevoClient nevoClient, ProductRepository productRepository, FoodProperties properties) {
    this.nevoClient = nevoClient;
    this.productRepository = productRepository;
    this.properties = properties;
  }

  @Transactional
  public Product enrichMissingMicros(Product product) {
    if (!properties.nevo().enabled()) {
      return product;
    }
    if (product == null || product.getName() == null || product.getName().isBlank()) {
      return product;
    }

    Set<String> existing =
        product.getNutrients().stream()
            .map(ProductNutrient::getNutrientCode)
            .collect(HashSet::new, HashSet::add, HashSet::addAll);

    boolean missingMicros =
        NevoMicronutrientCodes.CODES.stream().anyMatch(code -> !existing.contains(code));
    if (!missingMicros) {
      return product;
    }

    List<NevoMatchRequest.KnownMacro> macros =
        product.getNutrients().stream()
            .filter(
                n ->
                    Set.of(
                            "energy_kcal",
                            "protein",
                            "fat",
                            "carbohydrates",
                            "sugars",
                            "fiber",
                            "sodium")
                        .contains(n.getNutrientCode()))
            .map(
                n ->
                    new NevoMatchRequest.KnownMacro(
                        n.getNutrientCode(), n.getAmountPer100g(), n.getUnit()))
            .toList();

    NevoMatchRequest request =
        new NevoMatchRequest(
            product.getName(),
            product.getBrand(),
            null,
            List.of(),
            product.getIngredientsText(),
            macros);

    NevoMatchResponse match;
    try {
      match = nevoClient.matchBest(request);
    } catch (NevoUnavailableException ex) {
      log.warn("Skipping NEVO enrichment for product {}: {}", product.getId(), ex.getMessage());
      return product;
    }
    if (match == null || !match.matched() || match.confidence() == null) {
      return product;
    }
    String confidence = match.confidence().toUpperCase(Locale.ROOT);
    if (!AUTO_APPLY.contains(confidence)) {
      log.info(
          "Skipping low-confidence NEVO match {} ({}) for product {}",
          match.nevoCode(),
          confidence,
          product.getId());
      return product;
    }

    List<ProductNutrient> merged = new ArrayList<>(product.getNutrients());
    Set<String> present = new HashSet<>(existing);
    for (NevoMatchResponse.NevoNutrientDto nutrient : match.nutrients()) {
      if (nutrient == null
          || nutrient.code() == null
          || nutrient.amountPer100g() == null
          || !NevoMicronutrientCodes.CODES.contains(nutrient.code())) {
        continue;
      }
      if (present.contains(nutrient.code())) {
        continue;
      }
      ProductNutrient pn = new ProductNutrient();
      pn.setProductId(product.getId());
      pn.setNutrientCode(nutrient.code());
      pn.setAmountPer100g(nutrient.amountPer100g());
      pn.setUnit(nutrient.unit());
      pn.setSource("NEVO_ESTIMATE");
      pn.setSourceRef(match.nevoCode());
      pn.setConfidence(confidence);
      pn.setEstimated(true);
      merged.add(pn);
      present.add(nutrient.code());
    }
    product.replaceNutrients(merged);
    return productRepository.save(product);
  }
}
