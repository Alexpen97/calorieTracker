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

  public EnrichmentBackfillService(
      ProductRepository productRepository, ProductEnrichmentService enrichmentService) {
    this.productRepository = productRepository;
    this.enrichmentService = enrichmentService;
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
      long microCount =
          product.getNutrients().stream()
              .map(ProductNutrient::getNutrientCode)
              .filter(ProductEnrichmentService.MICRO_CODES::contains)
              .count();
      if (microCount >= 6) {
        continue;
      }
      scanned++;
      int before = product.getNutrients().size();
      try {
        Product updated = enrichmentService.enrichIfSparse(product);
        if (updated.getNutrients().size() > before) {
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
