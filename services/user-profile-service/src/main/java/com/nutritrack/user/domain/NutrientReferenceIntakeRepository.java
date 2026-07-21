package com.nutritrack.user.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutrientReferenceIntakeRepository
    extends JpaRepository<NutrientReferenceIntake, NutrientReferenceIntakeId> {

  List<NutrientReferenceIntake> findBySexAndAgeMinLessThanEqualAndAgeMaxGreaterThanEqual(
      Sex sex, Short ageForMin, Short ageForMax);
}
