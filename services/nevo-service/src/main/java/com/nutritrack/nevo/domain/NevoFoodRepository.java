package com.nutritrack.nevo.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NevoFoodRepository extends JpaRepository<NevoFood, String> {

  @Query(
      value =
          """
          SELECT * FROM nevo_food
          WHERE LOWER(search_document) LIKE CONCAT('%', LOWER(:term), '%')
             OR LOWER(food_name_en) LIKE CONCAT('%', LOWER(:term), '%')
             OR LOWER(COALESCE(food_name_nl, '')) LIKE CONCAT('%', LOWER(:term), '%')
             OR LOWER(COALESCE(synonym, '')) LIKE CONCAT('%', LOWER(:term), '%')
          ORDER BY food_name_en
          LIMIT :limit
          """,
      nativeQuery = true)
  List<NevoFood> searchByTerm(@Param("term") String term, @Param("limit") int limit);
}
