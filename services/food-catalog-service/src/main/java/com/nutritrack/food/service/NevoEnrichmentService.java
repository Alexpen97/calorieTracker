package com.nutritrack.food.service;

import com.nutritrack.food.domain.NutrientSource;
import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductNutrient;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.nevo.NevoClient;
import com.nutritrack.food.nevo.NevoMatchRequest;
import com.nutritrack.food.nevo.NevoMatchResponse;
import com.nutritrack.food.nevo.NevoMicronutrientCodes;
import com.nutritrack.food.nevo.NevoUnavailableException;
import com.nutritrack.food.cache.ProductCache;
import com.nutritrack.food.config.FoodProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
  private final ProductCache productCache;
  private final FoodProperties properties;

  public NevoEnrichmentService(
      NevoClient nevoClient,
      ProductRepository productRepository,
      ProductCache productCache,
      FoodProperties properties) {
    this.nevoClient = nevoClient;
    this.productRepository = productRepository;
    this.productCache = productCache;
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

    Map<String, ProductNutrient> byCode = new HashMap<>();
    for (ProductNutrient nutrient : product.getNutrients()) {
      byCode.put(nutrient.getNutrientCode(), nutrient);
    }
    if (!MicroEnrichmentGate.hasNevoGaps(byCode)) {
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
            product.getGenericName(),
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
    Set<String> filled = new HashSet<>(MicroEnrichmentGate.filledCodes(product));
    boolean changed = false;
    for (NevoMatchResponse.NevoNutrientDto nutrient : match.nutrients()) {
      if (nutrient == null
          || nutrient.code() == null
          || nutrient.amountPer100g() == null
          || !NevoMicronutrientCodes.CODES.contains(nutrient.code())) {
        continue;
      }
      if (filled.contains(nutrient.code())) {
        continue;
      }
      merged.removeIf(
          existing ->
              nutrient.code().equals(existing.getNutrientCode())
                  && !MicroEnrichmentGate.isFilled(existing));
      ProductNutrient pn = new ProductNutrient();
      pn.setProductId(product.getId());
      pn.setNutrientCode(nutrient.code());
      pn.setAmountPer100g(nutrient.amountPer100g());
      pn.setUnit(nutrient.unit());
      pn.setSource(NutrientSource.NEVO_ESTIMATE);
      pn.setSourceRef(match.nevoCode());
      pn.setConfidence(confidence);
      pn.setEstimated(true);
      merged.add(pn);
      filled.add(nutrient.code());
      changed = true;
    }
    if (!changed) {
      return product;
    }
    product.replaceNutrients(merged);
    Product saved = productRepository.save(product);
    if (saved.getBarcode() != null) {
      productCache.evictByBarcode(saved.getBarcode());
    }
    return saved;
  }
}
