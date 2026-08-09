package com.nutritrack.food.service;

import com.nutritrack.food.config.FoodProperties;
import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.domain.ProductSubmission;
import com.nutritrack.food.domain.ProductSubmissionRepository;
import com.nutritrack.food.domain.SubmissionStatus;
import com.nutritrack.food.nevo.NevoClient;
import com.nutritrack.food.nevo.NevoFoodSearchResponse;
import com.nutritrack.food.off.NormalizedOffProduct;
import com.nutritrack.food.off.OffClient;
import com.nutritrack.food.web.dto.ProductNutrientResponse;
import com.nutritrack.food.web.dto.ProductResponse;
import com.nutritrack.food.web.dto.ProductSearchResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductSearchService {

  private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

  private final ProductRepository productRepository;
  private final ProductSubmissionRepository submissionRepository;
  private final OffClient offClient;
  private final NevoClient nevoClient;
  private final OffProductUpsertService upsertService;
  private final ProductMapper productMapper;
  private final FoodProperties properties;

  public ProductSearchService(
      ProductRepository productRepository,
      ProductSubmissionRepository submissionRepository,
      OffClient offClient,
      NevoClient nevoClient,
      OffProductUpsertService upsertService,
      ProductMapper productMapper,
      FoodProperties properties) {
    this.productRepository = productRepository;
    this.submissionRepository = submissionRepository;
    this.offClient = offClient;
    this.nevoClient = nevoClient;
    this.upsertService = upsertService;
    this.productMapper = productMapper;
    this.properties = properties;
  }

  @Transactional
  public ProductSearchResponse search(String rawQuery, int page, UUID callerUserId) {
    String query = rawQuery == null ? "" : rawQuery.trim();
    if (query.length() < 2) {
      throw new IllegalArgumentException("Search query must be at least 2 characters");
    }
    int pageSize = properties.search().pageSize();
    int zeroBasedPage = Math.max(page, 1) - 1;

    Map<String, ProductResponse> merged = new LinkedHashMap<>();

    for (ProductResponse response : searchNevo(rawQuery, properties.search().nevoSearchLimit())) {
      merged.putIfAbsent("nevo:" + response.nevoCode(), response);
    }

    if (callerUserId != null) {
      List<ProductSubmission> own =
          submissionRepository.searchOwn(
              callerUserId,
              query,
              List.of(SubmissionStatus.PENDING, SubmissionStatus.REJECTED),
              PageRequest.of(0, pageSize));
      for (ProductSubmission submission : own) {
        ProductResponse response = productMapper.toResponse(submission);
        merged.put("sub:" + submission.getId(), response);
      }
    }

    List<Product> local =
        productRepository
            .searchByDocument(query, PageRequest.of(zeroBasedPage, pageSize))
            .getContent();
    for (Product product : local) {
      merged.put("prod:" + product.getId(), productMapper.toResponse(product));
    }

    if (merged.size() < properties.search().localMinResultsBeforeOffFallback()) {
      try {
        List<NormalizedOffProduct> remote = offClient.searchByName(query, page);
        for (NormalizedOffProduct offProduct : remote) {
          Product saved = upsertService.upsertFromOff(offProduct);
          merged.putIfAbsent("prod:" + saved.getId(), productMapper.toResponse(saved));
          if (merged.size() >= pageSize + 5) {
            break;
          }
        }
      } catch (RuntimeException ignored) {
        // Degrade to local/mirror-only when OFF search is unavailable.
      }
    }

    List<ProductResponse> items = new ArrayList<>(merged.values());
    if (items.size() > pageSize) {
      items = items.subList(0, pageSize);
    }
    return new ProductSearchResponse(query, page <= 0 ? 1 : page, pageSize, items);
  }

  private List<ProductResponse> searchNevo(String rawQuery, int limit) {
    try {
      List<NevoFoodSearchResponse.Item> items = nevoClient.searchFoods(rawQuery, limit);
      if (items == null || items.isEmpty()) {
        return List.of();
      }
      return items.stream()
          .filter(item -> item != null && item.nevoCode() != null && !item.nevoCode().isBlank())
          .map(this::toNevoResponse)
          .toList();
    } catch (RuntimeException ex) {
      log.warn("Skipping NEVO search for query '{}': {}", rawQuery, ex.getMessage());
      return List.of();
    }
  }

  private ProductResponse toNevoResponse(NevoFoodSearchResponse.Item item) {
    String nevoCode = item.nevoCode();
    return new ProductResponse(
        UUID.nameUUIDFromBytes(("nevo:" + nevoCode).getBytes(StandardCharsets.UTF_8)),
        null,
        null,
        "NEVO",
        preferredName(item),
        item.foodGroup(),
        null,
        null,
        null,
        null,
        null,
        null,
        List.of(),
        null,
        nevoCode,
        item.foodGroup(),
        item.nutrients().stream()
            .filter(
                nutrient ->
                    nutrient != null
                        && nutrient.code() != null
                        && nutrient.amountPer100g() != null)
            .map(
                nutrient ->
                    new ProductNutrientResponse(
                        nutrient.code(), nutrient.amountPer100g(), nutrient.unit(), false))
            .toList());
  }

  private static String preferredName(NevoFoodSearchResponse.Item item) {
    if (item.nameEn() != null && !item.nameEn().isBlank()) {
      return item.nameEn();
    }
    if (item.nameNl() != null && !item.nameNl().isBlank()) {
      return item.nameNl();
    }
    return "NEVO " + item.nevoCode();
  }
}
