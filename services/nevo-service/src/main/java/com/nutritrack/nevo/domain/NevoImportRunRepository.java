package com.nutritrack.nevo.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NevoImportRunRepository extends JpaRepository<NevoImportRun, UUID> {}
