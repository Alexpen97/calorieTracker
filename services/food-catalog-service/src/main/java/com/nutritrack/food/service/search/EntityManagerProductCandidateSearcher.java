package com.nutritrack.food.service.search;

import com.nutritrack.food.config.FoodProperties;
import com.nutritrack.food.domain.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntityManagerProductCandidateSearcher implements ProductCandidateSearcher {

  private static final String DOCUMENT_SQL = "LOWER(COALESCE(p.search_document, ''))";

  private final EntityManager entityManager;
  private final DataSource dataSource;
  private final FoodProperties.Search searchProperties;
  private final SearchQueryNormalizer normalizer = new SearchQueryNormalizer();
  private final ProductRelevanceScorer scorer;
  private volatile Boolean postgres;

  public EntityManagerProductCandidateSearcher(
      EntityManager entityManager, DataSource dataSource, FoodProperties properties) {
    this.entityManager = entityManager;
    this.dataSource = dataSource;
    this.searchProperties = properties.search();
    this.scorer = new ProductRelevanceScorer(searchProperties.similarityThreshold());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Product> findCandidates(NormalizedQuery query, int limit) {
    if (query == null || query.tokens().isEmpty() || limit <= 0) {
      return List.of();
    }
    if (isPostgres()) {
      return findPostgresCandidates(query, limit);
    }
    return findH2Candidates(query, limit);
  }

  private List<Product> findPostgresCandidates(NormalizedQuery query, int limit) {
    Map<UUID, Product> candidates = new LinkedHashMap<>();
    addAll(candidates, runPostgresFts(query.normalized(), limit));
    if (candidates.size() < searchProperties.fuzzyMinResults()) {
      addAll(candidates, runPostgresTrigram(query.normalized(), limit));
    }
    return limit(candidates, limit);
  }

  private List<Product> runPostgresFts(String queryText, int limit) {
    Query query =
        entityManager.createNativeQuery(
            """
            SELECT p.* FROM product p
            WHERE to_tsvector('english', coalesce(p.search_document, ''))
                  @@ websearch_to_tsquery('english', :q)
            ORDER BY ts_rank(
                to_tsvector('english', coalesce(p.search_document, '')),
                websearch_to_tsquery('english', :q)
            ) DESC, p.name ASC
            """,
            Product.class);
    query.setParameter("q", queryText);
    query.setMaxResults(limit);
    return resultList(query);
  }

  private List<Product> runPostgresTrigram(String queryText, int limit) {
    Query query =
        entityManager.createNativeQuery(
            """
            SELECT p.* FROM product p
            WHERE coalesce(p.search_document, '') % :q
               OR similarity(coalesce(p.search_document, ''), :q) >= :threshold
            ORDER BY similarity(coalesce(p.search_document, ''), :q) DESC, p.name ASC
            """,
            Product.class);
    query.setParameter("q", queryText);
    query.setParameter("threshold", searchProperties.similarityThreshold());
    query.setMaxResults(limit);
    return resultList(query);
  }

  private List<Product> findH2Candidates(NormalizedQuery query, int limit) {
    int fetchLimit = Math.max(limit, 1) * 5;
    Map<UUID, Product> candidates = new LinkedHashMap<>();
    addAll(candidates, filterScored(query, runH2TokenAndQuery(query, fetchLimit)));
    if (candidates.size() < searchProperties.fuzzyMinResults()) {
      addAll(candidates, filterScored(query, runH2AnchorQuery(query, fetchLimit)));
    }
    return limit(candidates, limit);
  }

  private List<Product> runH2TokenAndQuery(NormalizedQuery query, int limit) {
    StringBuilder sql = new StringBuilder("SELECT p.* FROM product p WHERE ");
    Map<String, String> parameters = new LinkedHashMap<>();
    for (int tokenIndex = 0; tokenIndex < query.tokens().size(); tokenIndex++) {
      if (tokenIndex > 0) {
        sql.append(" AND ");
      }
      sql.append("(");
      List<String> variants = tokenVariants(query.tokens().get(tokenIndex));
      for (int variantIndex = 0; variantIndex < variants.size(); variantIndex++) {
        if (variantIndex > 0) {
          sql.append(" OR ");
        }
        String parameterName = "t" + tokenIndex + "_" + variantIndex;
        sql.append(DOCUMENT_SQL).append(" LIKE :").append(parameterName);
        parameters.put(parameterName, likeValue(variants.get(variantIndex)));
      }
      sql.append(")");
    }
    sql.append(" ORDER BY p.name ASC");
    return runH2ProductQuery(sql.toString(), parameters, limit);
  }

  private List<Product> runH2AnchorQuery(NormalizedQuery query, int limit) {
    Set<String> anchors = h2FuzzyAnchors(query.tokens());
    if (anchors.isEmpty()) {
      return List.of();
    }

    StringBuilder sql = new StringBuilder("SELECT p.* FROM product p WHERE ");
    Map<String, String> parameters = new LinkedHashMap<>();
    int index = 0;
    for (String anchor : anchors) {
      if (index > 0) {
        sql.append(" OR ");
      }
      String parameterName = "anchor" + index++;
      sql.append(DOCUMENT_SQL).append(" LIKE :").append(parameterName);
      parameters.put(parameterName, likeValue(anchor));
    }
    sql.append(" ORDER BY p.name ASC");
    return runH2ProductQuery(sql.toString(), parameters, limit);
  }

  private List<Product> runH2ProductQuery(String sql, Map<String, String> parameters, int limit) {
    Query query = entityManager.createNativeQuery(sql, Product.class);
    for (Map.Entry<String, String> parameter : parameters.entrySet()) {
      query.setParameter(parameter.getKey(), parameter.getValue());
    }
    query.setMaxResults(limit);
    return resultList(query);
  }

  private List<Product> filterScored(NormalizedQuery query, List<Product> products) {
    List<Product> scored = new ArrayList<>();
    for (Product product : products) {
      if (scorer.score(query, product.getName(), product.getBrand(), product.getSearchDocument()) > 0) {
        scored.add(product);
      }
    }
    return scored;
  }

  private List<String> tokenVariants(String token) {
    List<String> variants = new ArrayList<>(normalizer.normalize(token).expandedTokens());
    variants.sort(Comparator.naturalOrder());
    return variants;
  }

  private Set<String> h2FuzzyAnchors(List<String> tokens) {
    Set<String> anchors = new LinkedHashSet<>();
    if (tokens.isEmpty()) {
      return anchors;
    }
    anchors.add(tokens.get(0));
    tokens.stream().max(Comparator.comparingInt(String::length)).ifPresent(anchors::add);
    return anchors;
  }

  private String likeValue(String token) {
    return "%" + token.toLowerCase(Locale.ROOT) + "%";
  }

  private void addAll(Map<UUID, Product> candidates, List<Product> products) {
    for (Product product : products) {
      candidates.putIfAbsent(product.getId(), product);
    }
  }

  private List<Product> limit(Map<UUID, Product> candidates, int limit) {
    List<Product> products = new ArrayList<>(candidates.values());
    if (products.size() <= limit) {
      return products;
    }
    return products.subList(0, limit);
  }

  @SuppressWarnings("unchecked")
  private List<Product> resultList(Query query) {
    return query.getResultList();
  }

  private boolean isPostgres() {
    Boolean cached = postgres;
    if (cached != null) {
      return cached;
    }
    try (Connection connection = dataSource.getConnection()) {
      boolean detected =
          connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgres");
      postgres = detected;
      return detected;
    } catch (SQLException ex) {
      throw new IllegalStateException("Unable to detect database product for product search", ex);
    }
  }
}
