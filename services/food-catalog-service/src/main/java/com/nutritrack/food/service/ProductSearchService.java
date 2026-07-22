package com.nutritrack.food.service;

import com.nutritrack.food.config.FoodProperties;
import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.domain.ProductSubmission;
import com.nutritrack.food.domain.ProductSubmissionRepository;
import com.nutritrack.food.domain.SubmissionStatus;
import com.nutritrack.food.off.NormalizedOffProduct;
import com.nutritrack.food.off.OffClient;
import com.nutritrack.food.web.dto.ProductResponse;
import com.nutritrack.food.web.dto.ProductSearchResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductSearchService {

  private final ProductRepository productRepository;
  private final ProductSubmissionRepository submissionRepository;
  private final OffClient offClient;
  private final OffProductUpsertService upsertService;
  private final ProductMapper productMapper;
  private final FoodProperties properties;

  public ProductSearchService(
      ProductRepository productRepository,
      ProductSubmissionRepository submissionRepository,
      OffClient offClient,
      OffProductUpsertService upsertService,
      ProductMapper productMapper,
      FoodProperties properties) {
    this.productRepository = productRepository;
    this.submissionRepository = submissionRepository;
    this.offClient = offClient;
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
}
