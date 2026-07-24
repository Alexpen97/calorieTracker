package com.nutritrack.nevo.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NevoNutrientValueRepository extends JpaRepository<NevoNutrientValue, UUID> {
  List<NevoNutrientValue> findByNevoCode(String nevoCode);

  void deleteByNevoCodeIn(Iterable<String> nevoCodes);
}
