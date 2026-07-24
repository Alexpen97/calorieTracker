package com.nutritrack.nevo.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NevoAliasRepository extends JpaRepository<NevoAlias, UUID> {
  List<NevoAlias> findAll();
}
