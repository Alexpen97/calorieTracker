package com.nutritrack.food.service;

import com.nutritrack.food.cache.ProductCache;
import com.nutritrack.food.domain.NutrientSource;
import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductNutrient;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.domain.ProductSource;
import com.nutritrack.food.enrichment.EnrichmentClient;
import com.nutritrack.food.enrichment.EnrichmentResult;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductEnrichmentService {

  private static final Logger log = LoggerFactory.getLogger(ProductEnrichmentService.class);

  public static final Set<String> MICRO_CODES =
      Set.of(
          "vitamin_a",
          "vitamin_b1",
          "vitamin_b2",
          "vitamin_b3",
          "vitamin_b5",
          "vitamin_b6",
          "vitamin_b7",
          "vitamin_b9",
          "vitamin_b12",
          "vitamin_c",
          "vitamin_d",
          "vitamin_e",
          "vitamin_k",
          "calcium",
          "iron",
          "magnesium",
          "phosphorus",
          "potassium",
          "sodium",
          "zinc",
          "copper",
          "iodine",
          "manganese",
          "selenium",
          "chromium",
          "molybdenum");

  private static final int SPARSE_THRESHOLD = 6;

  private final EnrichmentClient enrichmentClient;
  private final ProductRepository productRepository;
  private final ProductCache productCache;

  public ProductEnrichmentService(
      EnrichmentClient enrichmentClient,
      ProductRepository productRepository,
      ProductCache productCache) {
    this.enrichmentClient = enrichmentClient;
    this.productRepository = productRepository;
    this.productCache = productCache;
  }

  @Transactional
  public Product enrichIfSparse(Product product) {
    if (product == null || product.getSource() != ProductSource.OFF) {
      return product;
    }
    long microCount =
        product.getNutrients().stream()
            .map(ProductNutrient::getNutrientCode)
            .filter(MICRO_CODES::contains)
            .count();
    if (microCount >= SPARSE_THRESHOLD) {
      return product;
    }
    if (product.getBarcode() == null || product.getBarcode().isBlank()) {
      return product;
    }

    List<String> existing =
        product.getNutrients().stream().map(ProductNutrient::getNutrientCode).toList();

    try {
      return enrichmentClient
          .enrich(product.getBarcode(), product.getName(), product.getBrand(), existing)
          .map(result -> apply(product, result))
          .orElse(product);
    } catch (RuntimeException ex) {
      log.warn("Skipping enrichment for {}: {}", product.getBarcode(), ex.toString());
      return product;
    }
  }

  private Product apply(Product product, EnrichmentResult result) {
    if (result.nutrients() == null || result.nutrients().isEmpty()) {
      return product;
    }
    NutrientSource source = resolveSource(result.matchType());
    if (source == null) {
      return product;
    }
    Set<String> existing = new HashSet<>();
    for (ProductNutrient n : product.getNutrients()) {
      existing.add(n.getNutrientCode());
    }
    boolean changed = false;
    for (EnrichmentResult.Nutrient n : result.nutrients()) {
      if (n.code() == null || existing.contains(n.code()) || !MICRO_CODES.contains(n.code())) {
        continue;
      }
      ProductNutrient pn = new ProductNutrient();
      pn.setProductId(product.getId());
      pn.setNutrientCode(n.code());
      pn.setAmountPer100g(n.amountPer100g());
      pn.setUnit(n.unit());
      pn.setSource(source);
      pn.setSourceRef(result.fdcId() == null ? null : String.valueOf(result.fdcId()));
      pn.setConfidence(
          result.confidence() == null ? null : result.confidence().toPlainString());
      pn.setEstimated(true);
      pn.setProduct(product);
      product.getNutrients().add(pn);
      existing.add(n.code());
      changed = true;
    }
    if (!changed) {
      return product;
    }
    Product saved = productRepository.save(product);
    if (saved.getBarcode() != null) {
      productCache.evictByBarcode(saved.getBarcode());
    }
    return saved;
  }

  private static NutrientSource resolveSource(String matchType) {
    if (matchType == null) {
      return null;
    }
    return switch (matchType) {
      case "GTIN", "NAME_BRAND" -> NutrientSource.USDA_BRANDED;
      case "GENERIC_PROXY" -> NutrientSource.USDA_PROXY;
      default -> null;
    };
  }
}
