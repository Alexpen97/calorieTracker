package com.nutritrack.food.service.search;

import com.nutritrack.food.domain.Product;
import java.util.List;

public interface ProductCandidateSearcher {
  List<Product> findCandidates(NormalizedQuery query, int limit);
}
