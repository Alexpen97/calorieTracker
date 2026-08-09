package com.nutritrack.food.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {
  Optional<Product> findByBarcode(String barcode);

  @Query(
      """
      SELECT p FROM Product p
      WHERE LOWER(COALESCE(p.searchDocument, '')) LIKE LOWER(CONCAT('%', :q, '%'))
      ORDER BY
        CASE
          WHEN LOWER(p.name) = LOWER(:q) THEN 0
          WHEN LOWER(p.name) LIKE LOWER(CONCAT(:q, '%')) THEN 1
          ELSE 2
        END,
        p.name ASC
      """)
  @Deprecated(forRemoval = false)
  Page<Product> searchByDocument(@Param("q") String q, Pageable pageable);

  /**
   * Legacy naive name/brand LIKE match. Submission duplicate warnings use
   * {@link com.nutritrack.food.service.search.ProductCandidateSearcher} instead.
   */
  @Deprecated(forRemoval = false)
  @Query(
      """
      SELECT p FROM Product p
      WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :q, '%'))
      """)
  List<Product> findNameOrBrandMatches(@Param("q") String q, Pageable pageable);
}
