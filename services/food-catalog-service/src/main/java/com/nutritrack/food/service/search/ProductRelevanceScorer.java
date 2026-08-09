package com.nutritrack.food.service.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductRelevanceScorer {

  private final double similarityThreshold;
  private final SearchQueryNormalizer normalizer = new SearchQueryNormalizer();

  public ProductRelevanceScorer(double similarityThreshold) {
    this.similarityThreshold = similarityThreshold;
  }

  public double score(NormalizedQuery query, String name, String brand, String searchDocument) {
    if (query.tokens().isEmpty()) {
      return 0;
    }

    String normalizedName = normalizer.normalize(name).normalized();
    Set<String> documentTokens = new HashSet<>(normalizer.normalize(searchDocument).tokens());

    if (normalizedName.equals(query.normalized())) {
      return 1000;
    }

    String firstToken = query.tokens().get(0);
    if (normalizedName.startsWith(query.normalized()) || normalizedName.startsWith(firstToken)) {
      return 800;
    }

    int tokenHits = countTokenHits(query.tokens(), documentTokens);
    if (tokenHits == query.tokens().size()) {
      return 600 + tokenHits;
    }

    if (tokenHits >= 1) {
      return 300 + 50 * tokenHits;
    }

    String normalizedBrand = normalizer.normalize(brand).normalized();
    for (String token : query.tokens()) {
      if (normalizedBrand.contains(token)) {
        return 200;
      }
    }

    double bestSimilarity = bestTokenSimilarity(query.tokens(), documentTokens);
    if (bestSimilarity >= similarityThreshold) {
      return 100 * bestSimilarity;
    }

    return 0;
  }

  private int countTokenHits(List<String> queryTokens, Set<String> documentTokens) {
    int hits = 0;
    for (String token : queryTokens) {
      if (tokenMatchesDocument(token, documentTokens)) {
        hits++;
      }
    }
    return hits;
  }

  private boolean tokenMatchesDocument(String queryToken, Set<String> documentTokens) {
    for (String variant : normalizer.normalize(queryToken).expandedTokens()) {
      if (documentTokens.contains(variant)) {
        return true;
      }
    }
    return false;
  }

  private double bestTokenSimilarity(List<String> queryTokens, Set<String> documentTokens) {
    double best = 0;
    for (String queryToken : queryTokens) {
      for (String documentToken : documentTokens) {
        best = Math.max(best, EditDistance.normalizedSimilarity(queryToken, documentToken));
      }
    }
    return best;
  }
}
