package com.nutritrack.user.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserGoalRepository extends JpaRepository<UserGoal, UserGoalId> {

  @Query("select g from UserGoal g where g.user.id = :userId order by g.id.nutrientCode asc")
  List<UserGoal> findByUserIdOrderByNutrientCode(@Param("userId") UUID userId);

  @Query(
      """
      select g from UserGoal g
      where g.user.id = :userId and g.id.nutrientCode = :nutrientCode
      """)
  Optional<UserGoal> findByUserIdAndNutrientCode(
      @Param("userId") UUID userId, @Param("nutrientCode") String nutrientCode);
}
