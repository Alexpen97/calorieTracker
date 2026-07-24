package com.nutritrack.food.service;

import com.nutritrack.food.cache.ProductCache;
import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductNutrient;
import com.nutritrack.food.domain.NutrientSource;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.domain.ProductSource;
import com.nutritrack.food.domain.ProductSubmissionRepository;
import com.nutritrack.food.domain.SubmissionStatus;
import com.nutritrack.food.off.NormalizedOffProduct;
import com.nutritrack.food.off.OffClient;
import com.nutritrack.food.web.ProductNotFoundException;
import com.nutritrack.food.web.dto.ProductResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductLookupService {

  private final ProductRepository productRepository;
  private final ProductSubmissionRepository submissionRepository;
  private final ProductCache productCache;
  private final OffClient offClient;
  private final ProductMapper productMapper;
  private final OffProductUpsertService upsertService;
  private final ProductEnrichmentService enrichmentService;
  private final NevoEnrichmentService nevoEnrichmentService;

  public ProductLookupService(
      ProductRepository productRepository,
      ProductSubmissionRepository submissionRepository,
      ProductCache productCache,
      OffClient offClient,
      ProductMapper productMapper,
      OffProductUpsertService upsertService,
      ProductEnrichmentService enrichmentService,
      NevoEnrichmentService nevoEnrichmentService) {
    this.productRepository = productRepository;
    this.submissionRepository = submissionRepository;
    this.productCache = productCache;
    this.offClient = offClient;
    this.productMapper = productMapper;
    this.upsertService = upsertService;
    this.enrichmentService = enrichmentService;
    this.nevoEnrichmentService = nevoEnrichmentService;
  }

  @Transactional
  public ProductResponse lookupByBarcode(String rawBarcode, UUID callerUserId) {
    String barcode = sanitizeBarcode(rawBarcode);
    return productCache
        .getByBarcode(barcode)
        .orElseGet(
            () ->
                productRepository
                    .findByBarcode(barcode)
                    .map(
                        product -> {
                          ProductResponse response = productMapper.toResponse(product);
                          productCache.putByBarcode(barcode, response);
                          return response;
                        })
                    .or(() -> findOwnSubmissionByBarcode(barcode, callerUserId))
                    .orElseGet(() -> fetchPersistAndCache(barcode)));
  }

  @Transactional(readOnly = true)
  public ProductResponse getById(UUID id, UUID callerUserId) {
    return productRepository
        .findById(id)
        .map(productMapper::toResponse)
        .or(
            () ->
                submissionRepository
                    .findByIdAndSubmitterUserId(id, callerUserId)
                    .filter(
                        s ->
                            s.getStatus() == SubmissionStatus.PENDING
                                || s.getStatus() == SubmissionStatus.REJECTED
                                || s.getStatus() == SubmissionStatus.APPROVED)
                    .map(
                        s -> {
                          if (s.getPublishedProductId() != null) {
                            return productRepository
                                .findById(s.getPublishedProductId())
                                .map(productMapper::toResponse)
                                .orElseGet(() -> productMapper.toResponse(s));
                          }
                          return productMapper.toResponse(s);
                        }))
        .orElseThrow(() -> new ProductNotFoundException(id.toString()));
  }

  private java.util.Optional<ProductResponse> findOwnSubmissionByBarcode(
      String barcode, UUID callerUserId) {
    if (callerUserId == null) {
      return java.util.Optional.empty();
    }
    return submissionRepository
        .findFirstByBarcodeAndSubmitterUserIdAndStatusIn(
            barcode,
            callerUserId,
            List.of(SubmissionStatus.PENDING, SubmissionStatus.REJECTED))
        .map(productMapper::toResponse);
  }

  private ProductResponse fetchPersistAndCache(String barcode) {
    NormalizedOffProduct offProduct =
        offClient
            .fetchByBarcode(barcode)
            .orElseThrow(() -> new ProductNotFoundException(barcode));
    Product saved = upsertService.upsertFromOff(offProduct);
    // USDA first (branded/GTIN preferred), then NEVO for remaining gaps.
    saved = enrichmentService.enrichIfSparse(saved);
    saved = nevoEnrichmentService.enrichMissingMicros(saved);
    ProductResponse response = productMapper.toResponse(saved);
    productCache.putByBarcode(barcode, response);
    return response;
  }

  public static String sanitizeBarcode(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("Barcode is required");
    }
    String digits = raw.trim().replaceAll("\\s+", "");
    if (!digits.matches("\\d{8,14}")) {
      throw new IllegalArgumentException("Barcode must be 8–14 digits");
    }
    return digits;
  }

  /** Kept for callers that still map nutrients during tests of upsert. */
  static List<ProductNutrient> toNutrients(Product product, NormalizedOffProduct offProduct) {
    return offProduct.nutrients().stream()
        .map(
            n -> {
              ProductNutrient pn = new ProductNutrient();
              pn.setProductId(product.getId());
              pn.setNutrientCode(n.code());
              pn.setAmountPer100g(n.amountPer100g());
              pn.setUnit(n.unit());
              pn.setSource(NutrientSource.OFF);
              pn.setSourceRef(offProduct.barcode());
              pn.setEstimated(false);
              return pn;
            })
        .toList();
  }

  public static void applyOffFields(Product product, NormalizedOffProduct offProduct) {
    product.setSource(ProductSource.OFF);
    product.setName(offProduct.name());
    product.setBrand(offProduct.brand());
    product.setQuantityLabel(offProduct.quantityLabel());
    product.setServingSizeG(offProduct.servingSizeG());
    product.setImageUrl(offProduct.imageUrl());
    product.setNutriScore(offProduct.nutriScore());
    product.setIngredientsText(offProduct.ingredientsText());
    product.setAllergenTags(joinTags(offProduct.allergenTags()));
    product.setOffLastSyncedAt(Instant.now());
    product.refreshSearchDocument();
  }

  private static String joinTags(List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return null;
    }
    return String.join(",", tags);
  }
}
