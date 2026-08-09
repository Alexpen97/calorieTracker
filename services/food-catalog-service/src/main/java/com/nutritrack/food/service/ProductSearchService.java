package com.nutritrack.food.service;

import com.nutritrack.food.config.FoodProperties;
import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductSubmission;
import com.nutritrack.food.domain.ProductSubmissionRepository;
import com.nutritrack.food.domain.SubmissionStatus;
import com.nutritrack.food.off.NormalizedOffProduct;
import com.nutritrack.food.off.OffClient;
import com.nutritrack.food.service.search.NormalizedQuery;
import com.nutritrack.food.service.search.ProductCandidateSearcher;
import com.nutritrack.food.service.search.ProductRelevanceScorer;
import com.nutritrack.food.service.search.SearchQueryNormalizer;
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

  private final ProductSubmissionRepository submissionRepository;
  private final OffClient offClient;
  private final OffProductUpsertService upsertService;
  private final ProductMapper productMapper;
  private final FoodProperties properties;
  private final ProductCandidateSearcher candidateSearcher;
  private final SearchQueryNormalizer normalizer;
  private final ProductRelevanceScorer scorer;

  public ProductSearchService(
      ProductSubmissionRepository submissionRepository,
      OffClient offClient,
      OffProductUpsertService upsertService,
      ProductMapper productMapper,
      FoodProperties properties,
      ProductCandidateSearcher candidateSearcher) {
    this.submissionRepository = submissionRepository;
    this.offClient = offClient;
    this.upsertService = upsertService;
    this.productMapper = productMapper;
    this.properties = properties;
    this.candidateSearcher = candidateSearcher;
    this.normalizer = new SearchQueryNormalizer();
    this.scorer = new ProductRelevanceScorer(properties.search().similarityThreshold());
  }

  @Transactional
  public ProductSearchResponse search(String rawQuery, int page, UUID callerUserId) {
    String trimmedQuery = rawQuery == null ? "" : rawQuery.trim();
    if (trimmedQuery.length() < 2) {
      throw new IllegalArgumentException("Search query must be at least 2 characters");
    }
    NormalizedQuery query = normalizer.normalize(rawQuery);
    int pageSize = properties.search().pageSize();
    int zeroBasedPage = Math.max(page, 1) - 1;

    Map<String, ScoredProduct> scored = new LinkedHashMap<>();
    int sequence = 0;

    if (callerUserId != null) {
      List<ProductSubmission> own =
          submissionRepository.searchOwn(
              callerUserId,
              query.normalized(),
              List.of(SubmissionStatus.PENDING, SubmissionStatus.REJECTED),
              PageRequest.of(0, pageSize));
      for (ProductSubmission submission : own) {
        ProductResponse response = productMapper.toResponse(submission);
        scored.put(
            "sub:" + submission.getId(),
            new ScoredProduct(response, Double.POSITIVE_INFINITY, sequence++, true));
      }
    }

    for (Product product : candidateSearcher.findCandidates(query, pageSize * 3)) {
      double score =
          scorer.score(query, product.getName(), product.getBrand(), product.getSearchDocument());
      if (score > 0) {
        sequence = mergeProduct(scored, product, score, sequence);
      }
    }

    long catalogHits = scored.keySet().stream().filter(key -> key.startsWith("prod:")).count();
    if (catalogHits < properties.search().localMinResultsBeforeOffFallback()) {
      try {
        List<NormalizedOffProduct> remote = offClient.searchByName(query.normalized(), page);
        for (NormalizedOffProduct offProduct : remote) {
          Product saved = upsertService.upsertFromOff(offProduct);
          double score =
              scorer.score(query, saved.getName(), saved.getBrand(), saved.getSearchDocument());
          sequence = mergeProduct(scored, saved, score, sequence);
        }
      } catch (RuntimeException ignored) {
        // Degrade to local/mirror-only when OFF search is unavailable.
      }
    }

    List<ProductResponse> items =
        scored.values().stream().sorted(ProductSearchService::compareScoredProducts)
            .map(ScoredProduct::response)
            .toList();
    int fromIndex = Math.min(zeroBasedPage * pageSize, items.size());
    int toIndex = Math.min(fromIndex + pageSize, items.size());
    if (fromIndex > 0 || toIndex < items.size()) {
      items = new ArrayList<>(items.subList(fromIndex, toIndex));
    }
    return new ProductSearchResponse(trimmedQuery, page <= 0 ? 1 : page, pageSize, items);
  }

  private int mergeProduct(
      Map<String, ScoredProduct> scored, Product product, double score, int sequence) {
    String key = "prod:" + product.getId();
    ScoredProduct next = new ScoredProduct(productMapper.toResponse(product), score, sequence, false);
    ScoredProduct existing = scored.get(key);
    if (existing == null) {
      scored.put(key, next);
      return sequence + 1;
    }
    if (score > existing.score()) {
      scored.put(key, new ScoredProduct(next.response(), score, existing.sequence(), false));
    }
    return sequence;
  }

  private static int compareScoredProducts(ScoredProduct left, ScoredProduct right) {
    if (left.submission() != right.submission()) {
      return left.submission() ? -1 : 1;
    }
    if (left.submission()) {
      return Integer.compare(left.sequence(), right.sequence());
    }

    int byScore = Double.compare(right.score(), left.score());
    if (byScore != 0) {
      return byScore;
    }
    String leftName = left.response().name() == null ? "" : left.response().name();
    String rightName = right.response().name() == null ? "" : right.response().name();
    return String.CASE_INSENSITIVE_ORDER.compare(leftName, rightName);
  }

  private record ScoredProduct(
      ProductResponse response, double score, int sequence, boolean submission) {}
}
