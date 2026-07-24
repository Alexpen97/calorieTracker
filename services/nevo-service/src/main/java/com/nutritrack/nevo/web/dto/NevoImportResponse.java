package com.nutritrack.nevo.web.dto;

import java.time.Instant;
import java.util.UUID;

public record NevoImportResponse(
    UUID id,
    String csvFilename,
    String nevoVersion,
    int foodCount,
    int nutrientCount,
    String status,
    Instant startedAt,
    Instant finishedAt,
    String errorMessage) {}
