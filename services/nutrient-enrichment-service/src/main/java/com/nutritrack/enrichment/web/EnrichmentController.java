package com.nutritrack.enrichment.web;

import com.nutritrack.enrichment.config.EnrichmentProperties;
import com.nutritrack.enrichment.service.EnrichmentService;
import com.nutritrack.enrichment.web.dto.EnrichRequest;
import com.nutritrack.enrichment.web.dto.EnrichResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class EnrichmentController {

  private final EnrichmentService enrichmentService;
  private final EnrichmentProperties properties;

  public EnrichmentController(EnrichmentService enrichmentService, EnrichmentProperties properties) {
    this.enrichmentService = enrichmentService;
    this.properties = properties;
  }

  @PostMapping("/internal/enrich")
  public EnrichResponse enrich(
      @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
      @Valid @RequestBody EnrichRequest request) {
    if (apiKey == null || !apiKey.equals(properties.internalApiKey())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal API key");
    }
    return enrichmentService.enrich(request);
  }
}
