package com.nutritrack.food.service;

import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductNutrient;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.domain.ProductSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrichmentBackfillService {

  private final ProductRepository productRepository;
  private final ProductEnrichmentService enrichmentService;
  private final NevoEnrichmentService nevoEnrichmentService;

  public EnrichmentBackfillService(
      ProductRepository productRepository,
      ProductEnrichmentService enrichmentService,
      NevoEnrichmentService nevoEnrichmentService) {
    this.productRepository = productRepository;
    this.enrichmentService = enrichmentService;
    this.nevoEnrichmentService = nevoEnrichmentService;
  }

  @Transactional
  public Map<String, Object> backfill(int page, int size) {
    int scanned = 0;
    int enriched = 0;
    int failed = 0;
    List<Product> products =
        productRepository.findAll(PageRequest.of(page, size)).getContent();
    for (Product product : products) {
      if (product.getSource() != ProductSource.OFF) {
        continue;
      }
      if (!MicroEnrichmentGate.needsEnrichment(product)) {
        continue;
      }
      scanned++;
      int before = product.getNutrients().size();
      boolean hadEstimate = product.getNutrients().stream().anyMatch(ProductNutrient::isEstimated);
      try {
        Product updated = enrichmentService.enrichIfSparse(product);
        updated = nevoEnrichmentService.enrichMissingMicros(updated);
        boolean hasEstimate = updated.getNutrients().stream().anyMatch(ProductNutrient::isEstimated);
        if (updated.getNutrients().size() > before || (hasEstimate && !hadEstimate)) {
          enriched++;
        }
      } catch (RuntimeException ex) {
        failed++;
      }
    }
    Map<String, Object> result = new HashMap<>();
    result.put("scanned", scanned);
    result.put("enriched", enriched);
    result.put("failed", failed);
    result.put("page", page);
    result.put("size", size);
    return result;
  }
}
